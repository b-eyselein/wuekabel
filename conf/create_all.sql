create user if not exists 'wuekabel'@'localhost' identified by '1234';

create database if not exists wuekabel;

grant all on wuekabel.* to 'wuekabel'@'localhost';

flush privileges;