-- database: 'samudb'
-- username: 'samu'

CREATE TABLE users (
                       id serial PRIMARY KEY,
                       username varchar(50) NOT NULL UNIQUE CHECK (username <> ''),
                       password varchar(97) NOT NULL CHECK (password <> '')
);

CREATE TABLE data (
                      id serial PRIMARY KEY,
                      user_id integer NOT NULL,
                      label varchar(100) NOT NULL,
                      file varchar(100),
                      created_at timestamp NOT NULL
);


CREATE TABLE tokens (
                        id serial PRIMARY KEY,
                        user_id integer NOT NULL,
                        token varchar(110) NOT NULL,
                        expires_at timestamp NOT NULL
);


INSERT INTO users
(username, password)
VALUES
('samu', encode(sha256(sha256('samu')),'base64'));
