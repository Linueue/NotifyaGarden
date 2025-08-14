use std::sync::{Arc, Mutex};
use serde::{Serialize, Deserialize};
use serde_json::json;
use crate::message::State;
use crate::game::Data;

#[derive(Serialize)]
struct Document<'a>
{
    fields: &'a Data,
}

fn format_firestore(value: serde_json::Value, root_depth: i8) -> serde_json::Value
{
    match value
    {
        serde_json::Value::String(v) => {
            if root_depth < 2
            {
                return json!(v)
            }

            json!({ "stringValue": v })
        },
        serde_json::Value::Number(v) => {
            if v.is_i64() || v.is_u64()
            {
                json!({ "integerValue": v.to_string() })
            } else
            {
                json!({ "doubleValue": v })
            }
        },
        serde_json::Value::Bool(v)   => json!({ "booleanValue": v }),
        serde_json::Value::Null      => json!({ "nullValue": serde_json::Value::Null }),
        serde_json::Value::Array(arr) => json!({ "arrayValue": { "values": arr.into_iter().map(|v| format_firestore(v, root_depth + 1)).collect::<Vec<_>>() }}),
        serde_json::Value::Object(v) => {
            let fields = v
                .into_iter()
                .map(|(k, v)| (k, format_firestore(v, root_depth + 1)))
                .collect::<serde_json::Map<_, _>>();

            if root_depth >= 2
            {
                json!({ "mapValue": { "fields": fields }})
            } else
            {
                json!(fields)
            }
        },
    }
}

pub async fn api(state: Arc<Mutex<State>>)
{
    let url = "https://firestore.googleapis.com/v1/projects/notifyagarden/databases/(default)/documents/stocks";
    let state = state.lock().unwrap();

    let document = Document { fields: &state.data };
    let values = serde_json::to_value(&document).unwrap();
    let values = format_firestore(values, 0);
    let data = serde_json::to_string(&values).unwrap();

    let _ = reqwest::Client::new()
        .post(url)
        .body(data)
        .bearer_auth(&state.auth.access_token)
        .send()
        .await
        .unwrap();
}

#[tokio::test]
async fn get_documents()
{
    use crate::oauth2;

    #[derive(Serialize, Deserialize)]
    struct Collection
    {
        collectionId: String,
    }

    #[derive(Serialize, Deserialize)]
    struct FieldPath
    {
        fieldPath: String,
    }

    #[derive(Serialize, Deserialize)]
    struct Field
    {
        field: FieldPath,
        direction: String,
    }

    #[derive(Serialize, Deserialize)]
    struct StructuredQuery
    {
        from: Vec<Collection>,
        orderBy: Vec<Field>,
        limit: i32,
    }

    #[derive(Serialize, Deserialize)]
    struct Query
    {
        structuredQuery: StructuredQuery,
    }

    let url = "https://firestore.googleapis.com/v1/projects/notifyagarden/databases/(default)/documents:runQuery";
    let auth = oauth2::get_token().await;

    let query = Query {
        structuredQuery: StructuredQuery {
            from: vec![
                Collection {
                    collectionId: "stocks".to_string()
                }
            ],
            orderBy: vec![
                Field { 
                    field: FieldPath { 
                        fieldPath: "updated_at".to_string() 
                    }, 
                    direction: "DESCENDING".to_string(),
                },
            ],
            limit: 1,
        },
    };

    let query_string = serde_json::to_string(&query).unwrap();

    let r = reqwest::Client::new()
        .post(url)
        .bearer_auth(auth.access_token)
        .body(query_string)
        .send()
        .await
        .unwrap();

    println!("{}", r.text().await.unwrap());
}

#[tokio::test]
async fn formatting()
{
    use crate::{oauth2, game, message};
    let data = game::Data {
        egg_stock: Some(vec![game::Item {name: "bruh".to_string(), quantity: 1}]),
        gear_stock: None,
        seed_stock: None,
        eventshop_stock: None,
        weather: None,
        updated_at: 0,
    };
    let data = Document { fields: &data };

    let values = serde_json::to_value(data).unwrap();
    println!("{:?}", values);
    let values = format_firestore(values, 0);
    println!("{:?}", values);
    let values_string = serde_json::to_string_pretty(&values).unwrap();
    println!("{}", values_string);
}

#[tokio::test]
async fn restapi()
{
    use crate::{oauth2, game, message};
    let state = Arc::new(Mutex::new(message::State
    {
        auth: oauth2::get_token().await,
        current_time: std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap(),
        data: game::Data {
            egg_stock: Some(vec![game::Item {name: "bruh".to_string(), quantity: 1}]),
            gear_stock: None,
            seed_stock: None,
            eventshop_stock: None,
            weather: None,
            updated_at: 0,
        },
    }));
    api(state).await;
}
