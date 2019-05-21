# --- !Ups

insert into users (username, is_admin)
values ('first_user', true),
       ('second_user', true),
       ('third_user', true);

insert into user_passwords (username, password_hash)
values ('first_user', '$2a$10$6loikDKMzBkdP1HG33BeheyhF7e1.gNBx3mM1CiePRg2AaicJmj5.'),
       ('second_user', '$2a$10$Mjaar2EAS0olPjsWRNozcuEsST/IMhgt.iPJX9qBCDoBLmyv/WaSq'),
       ('third_user', '$2a$10$hQjCkndxdP7185BRjM8fJuFLr0UrySpyN4/dKhrjxr.9dAcMRqha.');

insert into courses(id, short_name, title)
values (1, 'prop_french_1', 'Propädeutikum Französisch 1');

insert into users_in_courses(username, course_id)
values ('first_user', 1);

insert into collections (id, course_id, name)
values (1, 1, 'La nature et la géographie'),
       (2, 1, 'Les plantes'),
       (3, 1, 'Les animaux'),
       (4, 1, 'L''être humain'),
       (5, 1, 'La famille');

# --- !Downs

delete ignore
from collections;

delete ignore
from users_in_courses;

delete ignore
from courses;

delete ignore
from user_passwords;

delete ignore
from users;
