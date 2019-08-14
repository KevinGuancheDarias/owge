import { ResourceManagerService } from './app/service/resource-manager.service';
import { LoginSessionService } from './app/login-session/login-session.service';
import { TestMetadataBuilder } from './helpers/test-metadata.builder';
// This file is required by karma.conf.js and loads recursively all the .spec and framework files

import 'zone.js/dist/long-stack-trace-zone';
import 'zone.js/dist/proxy.js';
import 'zone.js/dist/sync-test';
import 'zone.js/dist/jasmine-patch';
import 'zone.js/dist/async-test';
import 'zone.js/dist/fake-async-test';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting
} from '@angular/platform-browser-dynamic/testing';
import { MEDIA_ROUTES } from './app/config/config.pojo';
import { FakeClass } from './helpers/fake-class';
import { HttpClientTestingModule } from '@angular/common/http/testing';

// Unfortunately there's no typing for the `__karma__` variable. Just declare it as any.
declare var __karma__: any;
declare var require: any;

// Prevent Karma from running prematurely.
__karma__.loaded = function () { };

// CHANGE media root
const imagesRoot = 'http://192.168.99.100';
for (const key in MEDIA_ROUTES) {
  if (MEDIA_ROUTES.hasOwnProperty(key)) {
    MEDIA_ROUTES[key] = `${imagesRoot}${MEDIA_ROUTES[key]}`;
  }
}

// Register dependency maps
TestMetadataBuilder.registerDependencyGroup('BaseComponent', {
  declarations: [],
  providers: [LoginSessionService, ResourceManagerService],
  imports: []
});
TestMetadataBuilder.registerDependencyGroup('BaseHttpService', {
  providers: [
    { provide: LoginSessionService, useValue: FakeClass.getInstance(LoginSessionService) },
  ],
  imports: [
    HttpClientTestingModule
  ]
});
// END Register dependency maps

// First, initialize the Angular testing environment.
getTestBed().initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting()
);
// Then we find all the tests.
const context = require.context('./', true, /\.spec\.ts$/);
// And load the modules.
context.keys().map(context);
// Finally, start Karma to run the tests.
__karma__.start();
