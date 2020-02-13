package uk.gov.gchq.palisade.service.palisade.domain;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "data_request",
        uniqueConstraints = {@UniqueConstraint(columnNames = "request_id")},
        indexes = {@Index(name = "request", columnList = "request_id")})
public class DataRequestEntity {

    @Id
    @Column(name = "request_id", columnDefinition = "varchar(255)")
    private String requestId;

    @Column(name = "user", columnDefinition = "json")
    @Convert(attributeName = "user", converter = UserConverter.class)
    private User user;

    @Column(name = "context", columnDefinition = "json")
    @Convert(attributeName = "context", converter = ContextConverter.class)
    private Context context;

    @Transient
    private Map<LeafResource, Rules> leafResourceMap;

    public DataRequestEntity() { }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Map<LeafResource, Rules> getLeafResourceMap() {
        return Optional.ofNullable(leafResourceMap).orElseGet(Collections::emptyMap);
    }

    public void setLeafResourceMap(Map<LeafResource, Rules> leafResourceMap) {
        this.leafResourceMap = leafResourceMap;
    }
}
