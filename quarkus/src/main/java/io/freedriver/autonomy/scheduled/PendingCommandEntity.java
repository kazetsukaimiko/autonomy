package io.freedriver.autonomy.scheduled;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Row shape for {@code pending_command}. The ORM stays inactive so the pool
 * does not open before the store directory passes the trust check. The store
 * writes this table through JDBC after that check.
 */
@Entity
@Table(name = "pending_command")
public class PendingCommandEntity extends PanacheEntityBase {

    @Id
    public String id;

    public String target;

    public boolean state;

    public Instant dueAt;
}
