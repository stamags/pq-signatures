create table products_stores
(
    id int(10) UNSIGNED auto_increment,
    product_id int(10) UNSIGNED not null,
    store_id int(10) UNSIGNED not null,
    constraint products_stores_pk
        primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create unique index products_stores_uindex
    on products_stores (store_id, product_id);

