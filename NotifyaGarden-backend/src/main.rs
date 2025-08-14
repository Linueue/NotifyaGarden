use std::sync::{Arc, Mutex};

mod oauth2;
mod game;
mod message;
mod restapi;

#[tokio::main]
async fn main()
{
    let state = Arc::new(Mutex::new(message::State
    {
        auth: oauth2::get_token().await,
        current_time: std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap(),
        data: game::Data {
            egg_stock: None,
            gear_stock: None,
            seed_stock: None,
            eventshop_stock: None,
            weather: None,
            updated_at: 0,
        },
    }));

    game::listen(state).await;
}
