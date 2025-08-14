use serde::{Serialize, Deserialize};
use std::sync::{Arc, Mutex};
use crate::{oauth2, game, restapi};

#[derive(Serialize, Deserialize)]
struct Message
{
    data: GameData,
    topic: String,
}

#[derive(Serialize, Deserialize)]
struct MessageData
{
    message: Message,
}

pub struct State
{
    pub auth: oauth2::Token,
    pub current_time: std::time::Duration,
    pub data: game::Data,
}

#[derive(Serialize, Deserialize)]
struct GameData
{
    stocks: String,
}

pub async fn send_message(data: game::Data, state: Arc<Mutex<State>>)
{
    {
        let url = "https://fcm.googleapis.com/v1/projects/notifyagarden/messages:send";
        let time = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap();
        let mut state_mut = state.lock().unwrap();
        let diff = time - state_mut.current_time;
        let elapsed = diff.as_secs();

        if elapsed >= (60 * 60)
        {
            state_mut.auth = oauth2::get_token().await;
            state_mut.current_time = time;
        }

        let game_stocks = serde_json::to_string(&data).unwrap();

        let game_data = GameData
        {
            stocks: game_stocks,
        };

        let message = MessageData {
            message: Message {
                data: game_data,
                topic: "all".to_string(),
            }
        };

        let message_data = serde_json::to_string(&message).unwrap();

        reqwest::Client::new()
            .post(url)
            .body(message_data)
            .bearer_auth(&state_mut.auth.access_token)
            .send()
            .await
            .unwrap();
        }
    restapi::api(state.clone()).await;

    println!("Message sent.");
}
