package io.freedriver.autonomy.jsf.model;

import io.freedriver.autonomy.entity.jsonlink.BoardNameEntity;
import io.freedriver.autonomy.iface.Positional;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.Connector;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Named
@RequestScoped
public class ConnectorView {

    @Inject
    private ConnectorService connectors;

    public List<BoardNameEntity> getConnectors() {
        return connectors.allBoardNames()
                .stream()
                .sorted(Positional.EXPLICIT_ORDER)
                .collect(Collectors.toList());
    }
}