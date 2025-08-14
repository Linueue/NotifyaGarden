use serde::{Serialize, Deserialize};
use jsonwebtoken::{Header, Algorithm, EncodingKey};

#[derive(Deserialize, Debug)]
struct ServiceAccount
{
    client_email: String,
    private_key: String,
    token_uri: String,
}

#[derive(Serialize, Deserialize)]
pub struct Claims<'a>
{
    iss: &'a str,
    scope: &'a str,
    aud: &'a str,
    iat: i32,
    exp: i32,
}

#[derive(Deserialize, Debug)]
pub struct Token
{
    pub access_token: String,
    // pub expires_in: i32,
    // pub token_type: String,
}

pub async fn get_token() -> Token
{
    let auth = authorize("service-account-file.json").await;

    auth
}

pub async fn authorize(filename: &str) -> Token
{
    let file = std::fs::read_to_string(filename).expect("Could not read auth file!");
    let service_account: ServiceAccount = serde_json::from_str(&file).expect("Could not read auth file!");

    let time = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap();
    let hour = 3600;
    let exp = time + std::time::Duration::from_secs(hour);

    let claims = Claims {
        iss: &service_account.client_email,
        scope: "https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore",
        aud: &service_account.token_uri,
        iat: time.as_secs() as i32,
        exp: exp.as_secs() as i32,
    };

    let jwt = jsonwebtoken::encode(
        &Header::new(Algorithm::RS256),
        &claims,
        &EncodingKey::from_rsa_pem(service_account.private_key.as_bytes()).unwrap(),
    ).expect("Could not encode JWT.");

    let params = [
        ("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"),
        ("assertion", &jwt),
    ];

    let client = reqwest::Client::new();
    let response = client
        .post(service_account.token_uri)
        .form(&params)
        .send()
        .await
        .expect("Could not authorize server.");

    let response_json: Token = serde_json::from_str(&response.text().await.unwrap()).unwrap();

    response_json
}
