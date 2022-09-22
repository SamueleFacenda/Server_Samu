-- database: 'samudb'
-- username: 'samu'

-- create a table for storing usernames and passwords, and a table
-- for storing the user's data: the timestamp of creation of the record,
-- a 100 char string called 'label' and a 100 char string called 'file'
-- and the user id

CREATE TABLE users (
    id serial PRIMARY KEY,
    username varchar(50) NOT NULL UNIQUE CHECK (username <> ''),
    password varchar(50) NOT NULL CHECK (password <> '')
);

CREATE TABLE data (
    id serial PRIMARY KEY,
    user_id integer NOT NULL,
    label varchar(100) NOT NULL,
    file varchar(100) NOT NULL,
    created_at timestamp NOT NULL
);

INSERT INTO users (username, password) VALUES ('samu', 'samu');

ALTER TABLE users
    ALTER COLUMN password TYPE varchar(97),
    ALTER COLUMN password SET NOT NULL ;

CREATE TABLE tokens (
    id serial PRIMARY KEY,
    user_id integer NOT NULL,
    token varchar(100) NOT NULL,
    created_at timestamp NOT NULL
);

ALTER TABLE tokens
    ADD COLUMN expires_at timestamp NOT NULL;