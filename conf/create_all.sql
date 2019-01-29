create user if not exists 'wuekabel'@'%' identified by '1234';

create database if not exists wuekabel;

grant all on wuekabel.* to 'wuekabel'@'%';

flush privileges;