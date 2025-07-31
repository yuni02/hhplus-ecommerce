 create table balances
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                          not null,
    amount     decimal(15, 2) default 0.00     not null,
    status     varchar(20)    default 'ACTIVE' not null,
    created_at timestamp                       not null,
    updated_at timestamp                       not null,
    version    bigint         default 0        not null,
    constraint user_id
        unique (user_id)
);

create table coupons
(
    discount_amount decimal(38, 2) not null,
    issued_count    int            not null,
    total_quantity  int            not null,
    created_at      datetime(6)    not null,
    id              bigint auto_increment
        primary key,
    updated_at      datetime(6)    not null,
    valid_from      datetime(6)    null,
    valid_to        datetime(6)    null,
    status          varchar(20)    not null,
    description     varchar(255)   null,
    name            varchar(255)   not null
);

create table order_history_events
(
    discount_amount   int          null,
    discounted_amount int          not null,
    refund_amount     int          null,
    total_amount      int          not null,
    created_at        datetime(6)  not null,
    id                bigint auto_increment
        primary key,
    occurred_at       datetime(6)  not null,
    order_id          bigint       not null,
    event_type        varchar(50)  not null,
    payment_method    varchar(50)  null,
    cancel_reason     varchar(255) null
);

create table order_items
(
    quantity     int            not null,
    total_price  decimal(38, 2) not null,
    unit_price   decimal(38, 2) not null,
    id           bigint auto_increment
        primary key,
    order_id     bigint         not null,
    product_id   bigint         not null,
    product_name varchar(255)   not null
);

create table orders
(
    discounted_amount decimal(38, 2) null,
    discount_amount   decimal(38, 2) not null,
    total_amount      decimal(38, 2) not null,
    created_at        datetime(6)    not null,
    id                bigint auto_increment
        primary key,
    ordered_at        datetime(6)    not null,
    updated_at        datetime(6)    not null,
    user_coupon_id    bigint         null,
    user_id           bigint         not null,
    status            varchar(20)    not null,
    payment_method    varchar(50)    null
);

create table products
(
    current_price decimal(38, 2) not null,
    stock         int            not null,
    created_at    datetime(6)    not null,
    id            bigint auto_increment
        primary key,
    updated_at    datetime(6)    not null,
    status        varchar(20)    not null,
    category      varchar(255)   null,
    description   varchar(255)   null,
    name          varchar(255)   not null
);

create table product_stats
(
    conversion_rate    decimal(38, 2) null,
    date               date           not null,
    product_rank       int            null,
    quantity_sold      int            not null,
    revenue            decimal(38, 2) not null,
    total_sales_amount decimal(38, 2) null,
    total_sales_count  int            null,
    aggregation_date   datetime(6)    null,
    created_at         datetime(6)    not null,
    last_order_date    datetime(6)    null,
    product_id         bigint         not null,
    updated_at         datetime(6)    not null,
    primary key (date, product_id),
    constraint FKqawohfr96evam9fw5pt69rata
        foreign key (product_id) references products (id)
);

create table user_balance_tx
(
    amount           decimal(38, 2) not null,
    created_at       datetime(6)    not null,
    id               bigint auto_increment
        primary key,
    related_order_id bigint         null,
    updated_at       datetime(6)    not null,
    user_id          bigint         not null,
    status           varchar(20)    not null,
    tx_type          varchar(20)    not null,
    memo             varchar(255)   null
);

create table user_coupons
(
    discount_amount int         not null,
    coupon_id       bigint      not null,
    created_at      datetime(6) not null,
    id              bigint auto_increment
        primary key,
    issued_at       datetime(6) not null,
    order_id        bigint      null,
    updated_at      datetime(6) not null,
    used_at         datetime(6) null,
    user_id         bigint      not null,
    status          varchar(20) not null
);

create table users
(
    created_at datetime(6)  not null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6)  not null,
    user_id    bigint       null,
    status     varchar(20)  null,
    username   varchar(255) not null,
    constraint UKr43af9ap4edm43mmtq01oddj6
        unique (username)
);

create index idx_userid_status
    on users (user_id, status);

