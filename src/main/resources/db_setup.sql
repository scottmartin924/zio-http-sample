create database sample;

create table todo
(
    id         serial
        constraint todo_pk
            primary key,
    entry      text                                   not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null
);

create unique index todo_id_uindex
    on todo (id);

INSERT INTO public.todo (id, entry, created_at, updated_at) VALUES (1, 'Learn the first thing about zio');


create table user_credentials
(
    user_id  serial
        constraint user_credentials_pk
            primary key,
    username text not null,
    password text not null
);

INSERT INTO public.user_credentials (user_id, username, password) VALUES (1, 'scott', 'test');
INSERT INTO public.user_credentials (user_id, username, password) VALUES (2, 'max', 'woof');
INSERT INTO public.user_credentials (user_id, username, password) VALUES (3, 'emmy', 'bark');

create table user_role
(
    user_id integer not null
        constraint user_role_user_credentials_user_id_fk
            references user_credentials,
    role    text
);

INSERT INTO public.user_role (user_id, role) VALUES (1, 'admin');
INSERT INTO public.user_role (user_id, role) VALUES (2, 'test');
INSERT INTO public.user_role (user_id, role) VALUES (2, 'supervisor');
INSERT INTO public.user_role (user_id, role) VALUES (3, 'test');
INSERT INTO public.user_role (user_id, role) VALUES (2, 'moderator');
INSERT INTO public.user_role (user_id, role) VALUES (3, 'reader');
