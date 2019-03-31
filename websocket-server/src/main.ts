import { AuthenticatedSocketStorageService } from './service/authenticated-socket-storage.service';
import { ConfigurationService } from './service/configuration.service';
import { SocketService } from './service/socket.service';
import { AuthenticationService } from './service/authenticatio.service';

const configurationService: ConfigurationService = new ConfigurationService({
    host: '127.0.0.1',
    database: 'owgejava_account',
    user: 'root',
    password: '1234',
    charset: 'utf8'
});

new SocketService(
    new AuthenticationService(),
    new AuthenticatedSocketStorageService(),
    configurationService
);
