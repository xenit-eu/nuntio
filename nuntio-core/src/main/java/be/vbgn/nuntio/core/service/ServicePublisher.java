package be.vbgn.nuntio.core.service;

import be.vbgn.nuntio.core.service.Service.Identifier;

public interface ServicePublisher {

    void publish(Service service);

    void unpublish(Service.Identifier serviceIdentifier);

    Check registerCheck(Service.Identifier serviceIdentifier, CheckType type);

    void unregisterCheck(Identifier serviceIdentifier, CheckType checkType);
}
