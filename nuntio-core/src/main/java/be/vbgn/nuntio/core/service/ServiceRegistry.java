package be.vbgn.nuntio.core.service;

import be.vbgn.nuntio.core.service.Service.Identifier;
import java.util.Set;

public interface ServiceRegistry extends ServicePublisher {

    Set<Identifier> listServices();

}
