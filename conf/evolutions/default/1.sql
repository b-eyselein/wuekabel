# --- !Ups

-- Users

create table if not exists users (
    username varchar(50) primary key,
    is_admin boolean not null default false
);

-- User passwords

create table if not exists user_passwords (
    username      varchar(50) primary key,
    password_hash varchar(100) not null,

    foreign key (username) references users (username)
        on update cascade on delete cascade
);

-- Courses

create table if not exists courses (
    id         int primary key,
    short_name varchar(100) not null,
    title      varchar(100) not null
);

create table if not exists languages (
    id   int primary key,
    name varchar(100) not null
);

-- Users <-> Courses

create table if not exists users_in_courses (
    username  varchar(50),
    course_id int,

    primary key (username, course_id),
    foreign key (username) references users (username)
        on update cascade on delete cascade,
    foreign key (course_id) references courses (id)
        on update cascade on delete cascade
);

-- Collections

create table if not exists collections (
    id                int,
    course_id         int,
    front_language_id int          not null,
    back_language_id  int          not null,
    name              varchar(100) not null,

    primary key (id, course_id),
    foreign key (course_id) references courses (id)
        on update cascade on delete cascade,
    foreign key (front_language_id) references languages (id)
        on update cascade on delete cascade,
    foreign key (back_language_id) references languages (id)
        on update cascade on delete cascade
);

-- FlashCards with answers

create table if not exists flashcards (
    id              int,
    coll_id         int,
    course_id       int,
    flash_card_type enum ('Vocable', 'Text', 'Blank', 'Choice') not null default 'Vocable',
    front           text                                        not null,
    front_hint      text,
    back            text                                        not null,
    back_hint       text,

    primary key (id, coll_id, course_id),
    foreign key (coll_id, course_id) references collections (id, course_id)
        on update cascade on delete cascade
);

create table if not exists choice_answers (
    id          int,
    card_id     int,
    coll_id     int,
    course_id   int,

    answer      text                                  not null,
    correctness enum ('CORRECT', 'OPTIONAL', 'WRONG') not null default 'WRONG',

    primary key (id, card_id, coll_id, course_id),
    foreign key (card_id, coll_id, course_id) references flashcards (id, coll_id, course_id)
        on update cascade on delete cascade
);

create table if not exists blanks_answer_fragments (
    id        int,
    card_id   int,
    coll_id   int,
    course_id int,
    answer    text    not null,
    is_answer boolean not null default false,

    primary key (id, card_id, coll_id, course_id),
    foreign key (card_id, coll_id, course_id) references flashcards (id, coll_id, course_id)
        on update cascade on delete cascade
);

-- User <-> Flashcard

create table if not exists users_answered_flashcards (
    username      varchar(50),
    card_id       int,
    coll_id       int,
    course_id     int,
    front_to_back bool    not null default true,

    bucket        int     not null,
    date_answered date    not null,
    correct       boolean not null default false,
    tries         int     not null default 1,

    constraint bucket_check check (bucket between 0 and 6),

    primary key (username, card_id, coll_id, course_id, front_to_back),
    foreign key (username) references users (username)
        on update cascade on delete cascade,
    foreign key (card_id, coll_id, course_id) references flashcards (id, coll_id, course_id)
        on update cascade on delete cascade
);

-- Views

create view fronts_to_learn as
select id as card_id, fcs.coll_id, fcs.course_id, us.username, true as front_to_back
from flashcards fcs
         join users us
         left join (select *from users_answered_flashcards where front_to_back = true) as uaf
                   on us.username = uaf.username and uaf.card_id = fcs.id and uaf.coll_id = fcs.coll_id
where card_id is null;

create view backs_to_learn as
select id as card_id, fcs.coll_id, fcs.course_id, us.username, false as front_to_back
from flashcards fcs
         join users us
         left join (select * from users_answered_flashcards where front_to_back = false) as uaf
                   on us.username = uaf.username and uaf.card_id = fcs.id and uaf.coll_id = fcs.coll_id
where card_id is null;

create view flashcards_to_learn as
        (select * from fronts_to_learn)
        union all
        (select * from backs_to_learn);

create view flashcards_to_repeat as
select f.id                           as card_id,
       f.coll_id                      as coll_id,
       f.course_id                    as course_id,
       username,
       front_to_back,
       datediff(now(), date_answered) as time_since_answered
from flashcards f
         left join users_answered_flashcards uaf on uaf.card_id = f.id and uaf.coll_id = f.coll_id
where datediff(now(), date_answered) >= power(3, bucket)
   or (uaf.correct = false and uaf.tries < 2)
order by time_since_answered, front_to_back;

# --- !Downs

drop view if exists flashcards_to_repeat;

drop view if exists flashcards_to_learn;

drop view if exists backs_to_learn;

drop view if exists fronts_to_learn;

drop table if exists users_answered_flashcards;

drop table if exists blanks_answer_fragments;

drop table if exists choice_answers;

drop table if exists flashcards;

drop table if exists collections;

drop table if exists users_in_courses;

drop table if exists languages;

drop table if exists courses;

drop table if exists user_passwords;

drop table if exists users;
