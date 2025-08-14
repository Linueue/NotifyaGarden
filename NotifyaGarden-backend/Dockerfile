FROM rust:latest

WORKDIR /app

COPY service-account-file.json ./
COPY Cargo.toml Cargo.lock ./
COPY src ./src

RUN cargo build --release
ENV API_KEY = {API_KEY}

# COPY ./target/release/notifyagarden .

CMD ["./target/release/notifyagarden"]
