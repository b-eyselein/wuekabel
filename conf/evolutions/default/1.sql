# --- !Ups

-- Users

create table if not exists users
(
    username varchar(50) primary key,
    is_admin boolean not null default false
);

insert into users (username, is_admin)
values ('first_user', true),
       ('second_user', true),
       ('third_user', true);

-- User passwords

create table if not exists user_passwords
(
    username      varchar(50) primary key,
    password_hash varchar(100) not null,

    foreign key (username) references users (username)
        on update cascade on delete cascade
);

insert into user_passwords (username, password_hash)
values ('first_user', '$2a$10$6loikDKMzBkdP1HG33BeheyhF7e1.gNBx3mM1CiePRg2AaicJmj5.'),
       ('second_user', '$2a$10$Mjaar2EAS0olPjsWRNozcuEsST/IMhgt.iPJX9qBCDoBLmyv/WaSq'),
       ('third_user', '$2a$10$hQjCkndxdP7185BRjM8fJuFLr0UrySpyN4/dKhrjxr.9dAcMRqha.');

-- Courses

create table if not exists courses
(
    id         int primary key,
    short_name varchar(100) not null,
    title      varchar(100) not null
);

insert into courses(id, short_name, title)
values (1, 'prop_french_1', 'Propädeutikum Französisch 1');

-- Users <-> Courses

create table if not exists users_in_courses
(
    username  varchar(50),
    course_id int,

    primary key (username, course_id),
    foreign key (username) references users (username)
        on update cascade on delete cascade,
    foreign key (course_id) references courses (id)
        on update cascade on delete cascade
);

-- Collections

create table if not exists collections
(
    id        int,
    course_id int,
    name      varchar(100) not null,

    primary key (id, course_id),
    foreign key (course_id) references courses (id)
        on update cascade on delete cascade
);

insert into collections (id, course_id, name)
values (1, 1, 'Erste Sammlung'),
       (2, 1, 'Zweite Sammlung'),
       (3, 1, 'Dritte Sammlung');

-- FlashCards with answers

create table if not exists flashcards
(
    id              int,
    coll_id         int,
    course_id       int,
    flash_card_type enum ('Vocable', 'Text', 'Blank', 'Choice') not null default 'Vocable',
    question        text                                        not null,
    question_hint   text,
    meaning         text                                        not null,
    meaning_hint    text,

    primary key (id, coll_id, course_id),
    foreign key (coll_id, course_id) references collections (id, course_id)
        on update cascade on delete cascade
);

insert into flashcards (id, coll_id, course_id, flash_card_type, question, meaning)
values (1, 1, 1, 'Vocable', 'pater', 'Vater'),
       (2, 1, 1, 'Vocable', 'mater', 'Mutter'),
       (3, 1, 1, 'Text', 'magna domus', 'großes Haus'),
       (4, 1, 1, 'Choice', 'Welches Geschlecht hat das Wort pater?', ''),
       (5, 1, 1, 'Choice', 'Welche dieser Aussagen sind korrekt?', '');

create table if not exists choice_answers
(
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

insert into choice_answers (id, card_id, coll_id, course_id, answer, correctness)
values (1, 4, 1, 1, 'Maskulinum', 'CORRECT'),
       (2, 4, 1, 1, 'Femininum', 'WRONG'),
       (3, 4, 1, 1, 'Neutrum', 'WRONG'),
       (1, 5, 1, 1, 'Wörter auf -or sind meist männlich', 'CORRECT'),
       (2, 5, 1, 1, 'Wörter auf -is sind meist weiblich', 'WRONG'),
       (3, 5, 1, 1, 'Wörter auf -x sind meist weiblich', 'CORRECT'),
       (4, 5, 1, 1, 'Wörter auf -en sind immer neutrum', 'CORRECT');

create table if not exists blanks_answer_fragments
(
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

create table if not exists users_answered_flashcards
(
    username      varchar(50),
    card_id       int,
    coll_id       int,
    course_id     int,

    bucket        int     not null,
    date_answered date    not null,
    correct       boolean not null default false,
    tries         int     not null default 1,

    constraint bucket_check check (bucket between 0 and 6),

    primary key (username, card_id, coll_id, course_id),
    foreign key (username) references users (username)
        on update cascade on delete cascade,
    foreign key (card_id, coll_id, course_id) references flashcards (id, coll_id, course_id)
        on update cascade on delete cascade
);

-- Views

create view flashcards_to_learn as
select id as card_id, fcs.coll_id, fcs.course_id, us.username
from flashcards fcs
         join users us
         left outer join users_answered_flashcards uaf
                         on us.username = uaf.username and uaf.card_id = fcs.id and uaf.coll_id = fcs.coll_id
where card_id is null;

create view flashcards_to_repeat as
select f.id as card_id, f.coll_id as coll_id, f.course_id as course_id, username
from flashcards f
         left join users_answered_flashcards uaf on uaf.card_id = f.id and uaf.coll_id = f.coll_id
where datediff(now(), date_answered) >= power(3, bucket)
   or (uaf.correct = false and uaf.tries < 2);

# --- !Downs

drop view if exists flashcards_to_repeat;

drop view if exists flashcards_to_learn;

drop table if exists users_answered_flashcards;

drop table if exists buckets;

drop table if exists blanks_answer_fragments;

drop table if exists choice_answers;

drop table if exists flashcards;

drop table if exists collections;

drop table if exists users_in_courses;

drop table if exists courses;

drop table if exists user_passwords;

drop table if exists users;
