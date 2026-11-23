create table pending_command (
    id varchar(36) not null primary key,
    target varchar(255) not null,
    state boolean not null,
    due_at bigint not null
);

create table applied_list (
    list_id varchar(36) not null primary key,
    seen_at bigint not null
);
