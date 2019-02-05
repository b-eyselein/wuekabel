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
  id   varchar(50) primary key,
  name varchar(100) not null
);

-- Users <-> Courses

create table if not exists users_in_courses
(
  username  varchar(50),
  course_id varchar(50),

  primary key (username, course_id),
  foreign key (username) references users (username)
    on update cascade on delete cascade,
  foreign key (course_id) references courses (id)
    on update cascade on delete cascade
);

-- Language

create table if not exists languages
(
  id   int primary key auto_increment,
  name varchar(100) unique
);

insert into languages (id, name)
values (1, 'Latein'),
       (2, 'Französisch'),
       (3, 'Englisch');


-- Users <-> Languages

create table user_learns_language
(
  username varchar(50) not null,
  lang_id  int         not null,

  primary key (username, lang_id),
  foreign key (username) references users (username)
    on update cascade on delete cascade,
  foreign key (lang_id) references languages (id)
    on update cascade on delete cascade
);

insert into user_learns_language (username, lang_id)
values ('first_user', 1);

-- Buckets

create table if not exists buckets
(
  id            int primary key,
  distance_days int not null
);

-- distance_days == 3 ^ (id - 1)
insert into buckets (id, distance_days)
values (1, 1),
       (2, 3),
       (3, 9),
       (4, 27),
       (5, 81);

-- Flashcard collections

create table if not exists collections
(
  id      int          not null auto_increment,
  lang_id int          not null,
  name    varchar(100) not null,

  primary key (id, lang_id),
  foreign key (lang_id) references languages (id)
    on update cascade on delete cascade
);

insert into collections (id, lang_id, name)
values (1, 1, 'Beispielsammlung');

-- FlashCards with answers

create table if not exists flashcards
(
  id              int                                                                 not null auto_increment,
  coll_id         int                                                                 not null,
  lang_id         int                                                                 not null,
  flash_card_type enum ('Vocable', 'Text', 'Blank', 'SingleChoice', 'MultipleChoice') not null default 'Vocable',
  question        text                                                                not null,
  meaning         text, -- can be null!

  primary key (id, coll_id, lang_id),
  foreign key (coll_id, lang_id) references collections (id, lang_id)
    on update cascade on delete cascade
);

insert into flashcards (id, coll_id, lang_id, flash_card_type, question, meaning)
values (1, 1, 1, 'Vocable', 'pater', 'Vater'),
       (2, 1, 1, 'Vocable', 'mater', 'Mutter'),
       (3, 1, 1, 'Text', 'magna domus', 'großes Haus'),
       (4, 1, 1, 'SingleChoice', 'Welches Geschlecht hat das Wort pater?', null),
       (5, 1, 1, 'MultipleChoice', 'Welche dieser Aussagen sind korrekt?', null);

create table if not exists choice_answers
(
  id          int                                   not null auto_increment,
  card_id     int                                   not null,
  coll_id     int                                   not null,
  lang_id     int                                   not null,

  answer      text                                  not null,
  correctness enum ('CORRECT', 'OPTIONAL', 'WRONG') not null default 'WRONG',

  primary key (id, card_id, coll_id, lang_id),
  foreign key (card_id, coll_id, lang_id) references flashcards (id, coll_id, lang_id)
    on update cascade on delete cascade
);


insert into choice_answers (id, card_id, coll_id, lang_id, answer, correctness)
values (1, 4, 1, 1, 'Maskulinum', 'CORRECT'),
       (2, 4, 1, 1, 'Femininum', 'WRONG'),
       (3, 4, 1, 1, 'Neutrum', 'WRONG'),
       (1, 5, 1, 1, 'Wörter auf -or sind meist männlich', 'CORRECT'),
       (2, 5, 1, 1, 'Wörter auf -is sind meist weiblich', 'WRONG'),
       (3, 5, 1, 1, 'Wörter auf -x sind meist weiblich', 'CORRECT'),
       (4, 5, 1, 1, 'Wörter auf -en sind immer neutrum', 'CORRECT');

-- User <-> Flashcard

create table if not exists users_answered_flashcards
(
  username      varchar(50) not null,
  card_id       int         not null,
  coll_id       int         not null,
  lang_id       int         not null,
  bucket_id     int         not null,
  date_answered date        not null,
  correct       boolean     not null default false,
  tries         int         not null default 1,

  primary key (username, card_id, coll_id, lang_id),

  foreign key (username) references users (username)
    on update cascade on delete cascade,
  foreign key (card_id, coll_id, lang_id) references flashcards (id, coll_id, lang_id)
    on update cascade on delete cascade,
  foreign key (bucket_id) references buckets (id)
    on update cascade on delete cascade
);

# insert into users_answered_flashcards (username, card_id, coll_id, lang_id, bucket_id, date_answered, correct,
#                                        tries)
# values ('first_user', 1, 1, 1, 1, subdate(current_date(), 1), false, 1),
#        ('first_user', 2, 1, 1, 1, subdate(current_date(), 1), true, 2),
#        ('first_user', 3, 1, 1, 1, subdate(current_date(), 2), false, 1);

-- Views

create view flashcards_to_learn as
select id as card_id, fcs.coll_id, fcs.lang_id, us.username
from flashcards fcs
       join users us
       left outer join users_answered_flashcards uaf
                       on us.username = uaf.username and uaf.card_id = fcs.id and uaf.coll_id = fcs.coll_id
                         and uaf.lang_id = fcs.lang_id
where card_id is null;

create view flashcards_to_repeat as
select f.id as card_id, f.coll_id as coll_id, f.lang_id as lang_id, username, correct, tries
from flashcards f
       left join users_answered_flashcards uaf
                 on uaf.card_id = f.id and uaf.coll_id = f.coll_id and uaf.lang_id = f.lang_id
       join buckets b on uaf.bucket_id = b.id
where datediff(now(), date_answered) >= b.distance_days
   or (uaf.correct = false and uaf.tries < 2);

# --- !Downs

drop view if exists flashcards_to_repeat;

drop view if exists flashcards_to_learn;

drop table if exists users_answered_flashcards;

drop table if exists buckets;

drop table if exists choice_answers;

drop table if exists flashcards;

drop table if exists collections;

drop table if exists user_learns_language;

drop table if exists languages;

drop table if exists users_in_courses;

drop table if exists courses;

drop table if exists user_passwords;

drop table if exists users;
