/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quickstart.event.listener;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SysoutEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final RealmProvider model;
//    public SysoutEventListenerProvider(KeycloakSession session) {
//        this.session = session;
//        this.model = session.realms();
//    }

    private Set<EventType> excludedEvents;
    private Set<OperationType> excludedAdminOperations;

    public SysoutEventListenerProvider(KeycloakSession session, Set<EventType> excludedEvents, Set<OperationType> excludedAdminOpearations) {
        this.excludedEvents = excludedEvents;
        this.excludedAdminOperations = excludedAdminOpearations;
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {
        // Ignore excluded events
        if (excludedEvents != null && excludedEvents.contains(event.getType())) {
            return;
        } else {
            System.out.println("EVENT: " + toString(event));

            if (EventType.REGISTER.equals(event.getType())) {
                RealmModel realm = this.model.getRealm(event.getRealmId());
                UserModel user = this.session.users().getUserById(realm, event.getUserId());
                if (user != null) {
                    System.out.println("NEW USER HAS REGISTERED : " + event.getUserId());

                    user.setEnabled(false);

                    UserModel admin = new InMemoryUserAdapter(this.session, realm, "-1");
                    String emailAdmin = "gianpieroizzo@alia-space.com";
                    admin.setEmail(emailAdmin);
                    System.out.println("EmailFormActionFactory success, sent email to " + emailAdmin + " mentioning that " + user.getEmail() + " has registered!" );
                    EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
                    try {
                        emailSender.send(realm.getSmtpConfig(), admin, "Self Registration with Keycloak", "Hi Admin, a new user with the email "
                                        + user.getEmail() + " has just registered with keycloak! \n" +
                                        "To enable user go to https://keycloak.alia-space.com/auth/admin/master/console/#/realms/" + event.getRealmId() + "/users/" + event.getUserId() + " \n" +
                                        "This is an automatic notice.",
                                "<h3>Hi Admin,</h3>" +
                                        "<p>a new user with the email " + user.getEmail() + " has just registered with keycloak! </p>" +
                                        "<p>To enable user go to <a href=\"https://keycloak.alia-space.com/auth/admin/master/console/#/realms/" + event.getRealmId() + "/users/" + event.getUserId() + "\">user configuration</a></p>" +
                                        "<p><p>This is an automatic notice." );
                    } catch (EmailException e) {
                        System.out.println("EmailFormActionFactory success, could not send notification to admin: " +e);
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Ignore excluded operations
        if (excludedAdminOperations != null && excludedAdminOperations.contains(event.getOperationType())) {
            return;
        } else {
            System.out.println("EVENT: " + toString(event));
        }
    }

    private String toString(Event event) {
        StringBuilder sb = new StringBuilder();

        sb.append("type=");
        sb.append(event.getType());
        sb.append(", realmId=");
        sb.append(event.getRealmId());
        sb.append(", clientId=");
        sb.append(event.getClientId());
        sb.append(", userId=");
        sb.append(event.getUserId());
        sb.append(", ipAddress=");
        sb.append(event.getIpAddress());

        if (event.getError() != null) {
            sb.append(", error=");
            sb.append(event.getError());
        }

        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                sb.append(", ");
                sb.append(e.getKey());
                if (e.getValue() == null || e.getValue().indexOf(' ') == -1) {
                    sb.append("=");
                    sb.append(e.getValue());
                } else {
                    sb.append("='");
                    sb.append(e.getValue());
                    sb.append("'");
                }
            }
        }

        return sb.toString();
    }
    
    private String toString(AdminEvent adminEvent) {
        StringBuilder sb = new StringBuilder();

        sb.append("operationType=");
        sb.append(adminEvent.getOperationType());
        sb.append(", realmId=");
        sb.append(adminEvent.getAuthDetails().getRealmId());
        sb.append(", clientId=");
        sb.append(adminEvent.getAuthDetails().getClientId());
        sb.append(", userId=");
        sb.append(adminEvent.getAuthDetails().getUserId());
        sb.append(", ipAddress=");
        sb.append(adminEvent.getAuthDetails().getIpAddress());
        sb.append(", resourcePath=");
        sb.append(adminEvent.getResourcePath());

        if (adminEvent.getError() != null) {
            sb.append(", error=");
            sb.append(adminEvent.getError());
        }
        
        return sb.toString();
    }
    
    @Override
    public void close() {
    }

}
