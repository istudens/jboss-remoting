/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.remoting3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.security.sasl.Sasl;

import org.wildfly.common.Assert;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Property;
import org.xnio.Sequence;
import org.xnio.sasl.SaslQop;
import org.xnio.sasl.SaslStrength;

/**
 * Common options for Remoting configuration.
 */
public final class RemotingOptions {

    private static final String[] NO_STRINGS = new String[0];

    private RemotingOptions() {
    }

    /**
     * Merge the given option map into the given authentication configuration, and return the result.
     *
     * @param optionMap the option map (must not be {@code null})
     * @param authenticationConfiguration the authentication configuration (must not be {@code null})
     * @return the merged authentication configuration (not {@code null})
     */
    public static AuthenticationConfiguration mergeOptionsIntoAuthenticationConfiguration(OptionMap optionMap, AuthenticationConfiguration authenticationConfiguration) {
        Assert.checkNotNullParam("optionMap", optionMap);
        Assert.checkNotNullParam("authenticationConfiguration", authenticationConfiguration);

        final String protocol = optionMap.get(SASL_PROTOCOL);
        if (protocol != null) {
            authenticationConfiguration = authenticationConfiguration.useProtocol(protocol);
        }
        final String realm = optionMap.get(AUTH_REALM);
        if (realm != null) {
            authenticationConfiguration = authenticationConfiguration.useRealm(realm);
        }
        final String authzId = optionMap.get(AUTHORIZE_ID);
        if (authzId != null) {
            authenticationConfiguration = authenticationConfiguration.useAuthorizationName(authzId);
        }
        final Sequence<String> disallowedMechs = optionMap.get(Options.SASL_DISALLOWED_MECHANISMS);
        if (disallowedMechs != null) {
            authenticationConfiguration = authenticationConfiguration.forbidSaslMechanisms(disallowedMechs.toArray(NO_STRINGS));
        }
        final Sequence<String> mechanisms = optionMap.get(Options.SASL_MECHANISMS);
        if (mechanisms != null) {
            authenticationConfiguration = authenticationConfiguration.allowSaslMechanisms(mechanisms.toArray(NO_STRINGS));
        }
        final Map<String, String> saslPropertiesMap = new HashMap<>();
        final Sequence<Property> properties = optionMap.get(Options.SASL_PROPERTIES);
        if (properties != null) {
            for (Property property : properties) {
                // ELY-894
                //noinspection rawtypes,unchecked
                ((Map)saslPropertiesMap).put(property.getKey(), property.getValue());
            }
        }
        final Boolean forwardSecrecy = optionMap.get(Options.SASL_POLICY_FORWARD_SECRECY);
        if (forwardSecrecy != null) {
            saslPropertiesMap.put(Sasl.POLICY_FORWARD_SECRECY, forwardSecrecy.toString());
        }
        final Boolean noActive = optionMap.get(Options.SASL_POLICY_NOACTIVE);
        if (noActive != null) {
            saslPropertiesMap.put(Sasl.POLICY_NOACTIVE, noActive.toString());
        }
        final Boolean noAnonymous = optionMap.get(Options.SASL_POLICY_NOANONYMOUS);
        if (noAnonymous != null) {
            saslPropertiesMap.put(Sasl.POLICY_NOANONYMOUS, noAnonymous.toString());
        }
        final Boolean noDictionary = optionMap.get(Options.SASL_POLICY_NODICTIONARY);
        if (noDictionary != null) {
            saslPropertiesMap.put(Sasl.POLICY_NODICTIONARY, noDictionary.toString());
        }
        final Boolean noPlainText = optionMap.get(Options.SASL_POLICY_NOPLAINTEXT);
        if (noPlainText != null) {
            saslPropertiesMap.put(Sasl.POLICY_NOPLAINTEXT, noPlainText.toString());
        }
        final Boolean passCredentials = optionMap.get(Options.SASL_POLICY_PASS_CREDENTIALS);
        if (passCredentials != null) {
            saslPropertiesMap.put(Sasl.POLICY_PASS_CREDENTIALS, passCredentials.toString());
        }
        final Sequence<SaslQop> qop = optionMap.get(Options.SASL_QOP);
        if (qop != null) {
            final Iterator<SaslQop> iterator = qop.iterator();
            if (iterator.hasNext()) {
                StringBuilder b = new StringBuilder().append(iterator.next().getString());
                while (iterator.hasNext()) {
                    b.append(',').append(iterator.next().getString());
                }
                saslPropertiesMap.put(Sasl.QOP, b.toString());
            }
        }
        final Boolean reuse = optionMap.get(Options.SASL_REUSE);
        if (reuse != null) {
            saslPropertiesMap.put(Sasl.REUSE, reuse.toString());
        }
        final SaslStrength strength = optionMap.get(Options.SASL_STRENGTH);
        if (strength != null) {
            switch (strength) {
                case LOW: saslPropertiesMap.put(Sasl.STRENGTH, "low"); break;
                case MEDIUM: saslPropertiesMap.put(Sasl.STRENGTH, "medium"); break;
                case HIGH: saslPropertiesMap.put(Sasl.STRENGTH, "high"); break;
                default: throw Assert.impossibleSwitchCase(strength);
            }
        }
        final Boolean serverAuth = optionMap.get(Options.SASL_SERVER_AUTH);
        if (serverAuth != null) {
            saslPropertiesMap.put(Sasl.SERVER_AUTH, serverAuth.toString());
        }
        if (! saslPropertiesMap.isEmpty()) {
            authenticationConfiguration = authenticationConfiguration.useMechanismProperties(saslPropertiesMap);
        }
        return authenticationConfiguration;
    }

    /**
     * The size of the largest buffer that this endpoint will transmit over a connection.
     */
    public static final Option<Integer> SEND_BUFFER_SIZE = Option.simple(RemotingOptions.class, "SEND_BUFFER_SIZE", Integer.class);

    /**
     * The default send buffer size.
     */
    public static final int DEFAULT_SEND_BUFFER_SIZE = 8192;

    /**
     * The size of the largest buffer that this endpoint will accept over a connection.
     */
    public static final Option<Integer> RECEIVE_BUFFER_SIZE = Option.simple(RemotingOptions.class, "RECEIVE_BUFFER_SIZE", Integer.class);

    /**
     * The default receive buffer size.
     */
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 8192;

    /**
     * The size of allocated buffer regions.
     */
    public static final Option<Integer> BUFFER_REGION_SIZE = Option.simple(RemotingOptions.class, "BUFFER_REGION_SIZE", Integer.class);

    /**
     * The maximum window size of the transmit direction for connection channels, in bytes.
     */
    public static final Option<Integer> TRANSMIT_WINDOW_SIZE = Option.simple(RemotingOptions.class, "TRANSMIT_WINDOW_SIZE", Integer.class);

    /**
     * The default requested window size of the transmit direction for incoming channel open attempts.
     */
    public static final int INCOMING_CHANNEL_DEFAULT_TRANSMIT_WINDOW_SIZE = 0x20000;

    /**
     * The default requested window size of the transmit direction for outgoing channel open attempts.
     */
    public static final int OUTGOING_CHANNEL_DEFAULT_TRANSMIT_WINDOW_SIZE = Integer.MAX_VALUE;

    /**
     * The maximum window size of the receive direction for connection channels, in bytes.
     */
    public static final Option<Integer> RECEIVE_WINDOW_SIZE = Option.simple(RemotingOptions.class, "RECEIVE_WINDOW_SIZE", Integer.class);

    /**
     * The default requested window size of the receive direction for incoming channel open attempts.
     */
    public static final int INCOMING_CHANNEL_DEFAULT_RECEIVE_WINDOW_SIZE = 0x20000;

    /**
     * The default requested window size of the receive direction for outgoing channel open attempts.
     */
    public static final int OUTGOING_CHANNEL_DEFAULT_RECEIVE_WINDOW_SIZE = 0x20000;

    /**
     * The maximum number of outbound channels to support for a connection.
     */
    public static final Option<Integer> MAX_OUTBOUND_CHANNELS = Option.simple(RemotingOptions.class, "MAX_OUTBOUND_CHANNELS", Integer.class);

    /**
     * The default maximum number of outbound channels.
     */
    public static final int DEFAULT_MAX_OUTBOUND_CHANNELS = 40;

    /**
     * The maximum number of inbound channels to support for a connection.
     */
    public static final Option<Integer> MAX_INBOUND_CHANNELS = Option.simple(RemotingOptions.class, "MAX_INBOUND_CHANNELS", Integer.class);

    /**
     * The default maximum number of inbound channels.
     */
    public static final int DEFAULT_MAX_INBOUND_CHANNELS = 40;

    /**
     * The SASL authorization ID.  Used as authentication user name to use if no authentication {@code CallbackHandler} is specified
     * and the selected SASL mechanism demands a user name.
     */
    public static final Option<String> AUTHORIZE_ID = Option.simple(RemotingOptions.class, "AUTHORIZE_ID", String.class);

    /**
     * Deprecated alias for {@link #AUTHORIZE_ID}.
     */
    @Deprecated
    public static final Option<String> AUTH_USER_NAME = AUTHORIZE_ID;

    /**
     * The authentication realm to use if no authentication {@code CallbackHandler} is specified.
     */
    public static final Option<String> AUTH_REALM = Option.simple(RemotingOptions.class, "AUTH_REALM", String.class);

    /**
     * Specify the number of times a client is allowed to retry authentication before closing the connection.
     */
    public static final Option<Integer> AUTHENTICATION_RETRIES = Option.simple(RemotingOptions.class, "AUTHENTICATION_RETRIES", Integer.class);

    /**
     * The default number of authentication retries.
     */
    public static final int DEFAULT_AUTHENTICATION_RETRIES = 3;

    /**
     * The maximum number of concurrent outbound messages on a channel.
     */
    public static final Option<Integer> MAX_OUTBOUND_MESSAGES = Option.simple(RemotingOptions.class, "MAX_OUTBOUND_MESSAGES", Integer.class);

    /**
     * The default maximum number of concurrent outbound messages on an incoming channel.
     */
    public static final int INCOMING_CHANNEL_DEFAULT_MAX_OUTBOUND_MESSAGES = 80;

    /**
     * The default maximum number of concurrent outbound messages on an outgoing channel.
     */
    public static final int OUTGOING_CHANNEL_DEFAULT_MAX_OUTBOUND_MESSAGES = 0xffff;

    /**
     * The maximum number of concurrent inbound messages on a channel.
     */
    public static final Option<Integer> MAX_INBOUND_MESSAGES = Option.simple(RemotingOptions.class, "MAX_INBOUND_MESSAGES", Integer.class);

    /**
     * The default maximum number of concurrent inbound messages on a channel.
     */
    public static final int DEFAULT_MAX_INBOUND_MESSAGES = 80;

    /**
     * The interval to use for connection heartbeat, in milliseconds.  If the connection is idle in the outbound direction
     * for this amount of time, a ping message will be sent, which will trigger a corresponding reply message.
     */
    public static final Option<Integer> HEARTBEAT_INTERVAL = Option.simple(RemotingOptions.class, "HEARTBEAT_INTERVAL", Integer.class);

    /**
     * The default heartbeat interval.
     */
    public static final int DEFAULT_HEARTBEAT_INTERVAL = Integer.MAX_VALUE;

    /**
     * The maximum inbound message size to be allowed.  Messages exceeding this size will cause an exception to be thrown
     * on the reading side as well as the writing side.
     */
    public static final Option<Long> MAX_INBOUND_MESSAGE_SIZE = Option.simple(RemotingOptions.class, "MAX_INBOUND_MESSAGE_SIZE", Long.class);

    /**
     * The default maximum inbound message size.
     */
    public static final long DEFAULT_MAX_INBOUND_MESSAGE_SIZE = Long.MAX_VALUE;

    /**
     * The maximum outbound message size to send.  No messages larger than this well be transmitted; attempting to do
     * so will cause an exception on the writing side.
     */
    public static final Option<Long> MAX_OUTBOUND_MESSAGE_SIZE = Option.simple(RemotingOptions.class, "MAX_OUTBOUND_MESSAGE_SIZE", Long.class);

    /**
     * The default maximum outbound message size.
     */
    public static final long DEFAULT_MAX_OUTBOUND_MESSAGE_SIZE = Long.MAX_VALUE;

    /**
     * The server side of the connection passes it's name to the client in the initial greeting, by default the name is
     * automatically discovered from the local address of the connection or it can be overridden using this {@code Option}.
     */
    public static final Option<String> SERVER_NAME = Option.simple(RemotingOptions.class, "SERVER_NAME", String.class);

    /**
     * Where a {@code SaslServer} or {@code SaslClient} are created by default the protocol specified it 'remoting', this
     * {@code Option} can be used to override this.
     */
    public static final Option<String> SASL_PROTOCOL = Option.simple(RemotingOptions.class, "SASL_PROTOCOL", String.class);

    /**
     * The default SASL protocol name.
     */
    public static final String DEFAULT_SASL_PROTOCOL = "remote";
}
