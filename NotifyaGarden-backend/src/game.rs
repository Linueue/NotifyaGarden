use tokio_tungstenite::connect_async;
use tokio_tungstenite::tungstenite::client::IntoClientRequest;
use serde::{Serialize, Deserialize};
use futures_util::StreamExt;
use std::sync::{Arc, Mutex};
use crate::message::State;
use crate::message;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct Item
{
    #[serde(rename = "display_name")]
    pub name: String,
    pub quantity: i32,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct Weather
{
    #[serde(skip_serializing)]
    active: bool,
    #[serde(rename = "weather_name")]
    name: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Data
{
    #[serde(skip_serializing_if = "Option::is_none")]
    pub egg_stock: Option<Vec<Item>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub gear_stock: Option<Vec<Item>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub seed_stock: Option<Vec<Item>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub eventshop_stock: Option<Vec<Item>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub weather: Option<Vec<Weather>>,

    #[serde(skip_deserializing)]
    pub updated_at: u64,
}

fn copy_data(data: &mut Data, to_copy: &Data)
{
    if let Some(egg_stock) = &to_copy.egg_stock
    {
        data.egg_stock = Some(egg_stock.to_vec());
    }
    if let Some(gear_stock) = &to_copy.gear_stock
    {
        data.gear_stock = Some(gear_stock.to_vec());
    }
    if let Some(seed_stock) = &to_copy.seed_stock
    {
        data.seed_stock = Some(seed_stock.to_vec());
    }
    if let Some(eventshop_stock) = &to_copy.eventshop_stock
    {
        data.eventshop_stock = Some(eventshop_stock.to_vec());
    }
    if let Some(weather) = &to_copy.weather
    {
        data.weather = Some(weather.to_vec());
    }
    data.updated_at = to_copy.updated_at;
}

fn deduplicate_eggs(eggs: &Vec<Item>) -> Option<Vec<Item>>
{
    let mut deduplicated_eggs: Vec<Item> = Vec::with_capacity(3);
    let mut idx: std::collections::HashMap<&String, usize> = std::collections::HashMap::with_capacity(3);

    for i in 0..eggs.len()
    {
        if let Some(value) = idx.get(&eggs[i].name)
        {
            deduplicated_eggs[*value].quantity += 1;
            continue;
        }

        deduplicated_eggs.push(eggs[i].clone());
        idx.insert(&eggs[i].name, deduplicated_eggs.len() - 1);
    }

    Some(deduplicated_eggs)
}

fn format_weather(weather: &mut Weather)
{
    let mut name: String = "".to_string();
    let mut is_prev_uppercase = false;
    let chars = weather.name.as_bytes();

    for i in 0..chars.len()
    {
        if chars[i].is_ascii_uppercase()
        {
            if is_prev_uppercase && !chars[i + 1].is_ascii_uppercase() ||
              !is_prev_uppercase && i != 0
            {
                name += " ";
            }
            is_prev_uppercase = true;
        } else
        {
            is_prev_uppercase = false;
        }

        let ch_string = (chars[i] as char).to_string();
        name += &ch_string;
    }

    weather.name = name;
}

fn get_active_weather(weathers: &Vec<Weather>) -> Option<Vec<Weather>>
{
    let mut active_weather: Vec<Weather> = vec![];

    for weather in weathers
    {
        if weather.active
        {
            active_weather.push(weather.clone());
        }
    }

    if active_weather.is_empty()
    {
        active_weather.push(Weather { active: true, name: "Normal".to_string() });
    }

    Some(active_weather)
}

fn get_key() -> String
{
    let key = "API_KEY";

    std::env::var(key).expect("API_KEY environment variable does not exist!")
}

pub async fn listen(state: Arc<Mutex<State>>)
{
    //let url = "wss://ws.growagardenpro.com/";
    let url = "wss://websocket.joshlei.com/growagarden";
    let key = get_key();
    let mut running = true;
    let mut try_connect = 0;
    let max_try_connect = 5;

    while running
    {
        let mut initial_data = true;
        let mut request = url.into_client_request().unwrap();
        request.headers_mut().insert("jstudio-key", http::HeaderValue::from_str(key).unwrap());

        match connect_async(request).await
        {
            Ok((mut socket, _)) => {
                println!("Listening to {}", url);

                loop
                {
                    if let Some(Ok(response)) = socket.next().await {
                        let response_body = response.to_text().unwrap();
                        if response_body.is_empty()
                        {
                            continue;
                        }
                        let text: Option<Data> = match serde_json::from_str(response_body)
                        {
                            Ok(data) => Some(data),
                            Err(_) => { eprintln!("Got response: '{}'", response.to_text().unwrap()); None }
                        };

                        if text.is_none()
                        {
                            continue;
                        }

                        let mut text = text.unwrap();

                        let mut empty_count = 0i32;

                        let mut stock_fn = |stock: &mut Option<Vec<Item>>| {
                            if stock.is_none() || stock.as_ref().unwrap().is_empty()
                            {
                                empty_count += 1;
                            }
                        };
                        stock_fn(&mut text.seed_stock);
                        stock_fn(&mut text.gear_stock);
                        stock_fn(&mut text.egg_stock);
                        stock_fn(&mut text.eventshop_stock);

                        if text.weather.is_none() || text.weather.as_ref().unwrap().is_empty()
                        {
                            empty_count += 1;
                        }

                        if empty_count >= 5
                        {
                            println!("All of it are empty!");
                            continue;
                        }

                        text.egg_stock = if !text.egg_stock.is_none() {
                            deduplicate_eggs(&text.egg_stock.unwrap())
                        } else {
                            None
                        };

                        let active_weather = if !text.weather.is_none() {
                            get_active_weather(&text.weather.unwrap())
                        } else {
                            None
                        };
                        text.weather = active_weather;

                        if let Some(weathers) = text.weather.as_mut()
                        {
                            for mut weather in weathers
                            {
                                format_weather(&mut weather);
                            }
                        }

                        text.updated_at = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as u64;

                        if initial_data
                        {
                            state.lock().unwrap().data = text;
                            initial_data = false;
                            continue;
                        }

                        {
                            copy_data(&mut state.lock().unwrap().data, &text);
                        }

                        message::send_message(text, state.clone()).await;
                    } else
                    {
                        println!("Server has disconnected. Reconnecting...");
                        try_connect += 1;

                        if try_connect >= max_try_connect
                        {
                            println!("Failed to connect {} times.", try_connect);
                            running = false;
                        }

                        tokio::time::sleep(std::time::Duration::from_secs(5)).await;

                        break;
                    }
                }
            },
            Err(e) => {
                println!("Could not connect to test.");
                println!("Error: {}", e);
                running = false;
            }
        }
    }
}

