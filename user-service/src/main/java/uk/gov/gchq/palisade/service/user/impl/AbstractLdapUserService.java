/*
 * Copyright 2018-2021 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.palisade.service.user.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.common.UserId;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.service.UserService;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <p>
 * An {@code AbstractLdapUserService} connects to LDAP to lookup users.
 * </p>
 * To use this LDAP user service you will
 * need to extend this class and implement 3 methods:
 * <ul>
 * <li>
 * {@link #getAttributeNames()}
 * </li>
 * <li>
 * {@link #getAuths(UserId, Map, LdapContext)}
 * </li>
 * <li>
 * {@link #getRoles(UserId, Map, LdapContext)}
 * </li>
 * </ul>
 * <p>
 * This implementation does not allow you to add users.
 * </p>
 */
public abstract class AbstractLdapUserService implements UserService {
    protected static final String[] ESCAPED_CHARS = new String[]{"\\", "#", "+", "<", ">", ";", "\"", "@", "(", ")", "*", "="};
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLdapUserService.class);
    protected final LdapContext context;
    private final String ldapConfigPath;

    /**
     * Constructs a AbstractLdapUserService with a given {@link LdapContext}.
     * The cache time to live with be set the default or previously set value.
     *
     * @param context the {@link LdapContext} for making calls to LDAP.
     */
    public AbstractLdapUserService(final LdapContext context) {
        this.context = context;
        this.ldapConfigPath = null;
    }

    /**
     * <p>
     * Constructs a {@link AbstractLdapUserService} with a given path to {@link LdapContext}.
     * </p>
     *
     * @param ldapConfigPath the path to config for initializing {@link LdapContext} for making calls to LDAP. This can be a path to a file or a resource.
     * @throws IOException     if IO issues occur whilst loading the LDAP config.
     * @throws NamingException if a naming exception is encountered whilst constructing the LDAP context
     */
    public AbstractLdapUserService(final String ldapConfigPath)
            throws IOException, NamingException {
        requireNonNull(ldapConfigPath, "ldapConfigPath is required");
        this.ldapConfigPath = ldapConfigPath;
        this.context = createContext(ldapConfigPath);
        requireNonNull(context, "Unable to construct ldap context from: " + ldapConfigPath);
    }

    @Override
    public User getUser(final String userId) {
        requireNonNull(userId, "userId is null");
        LOGGER.debug("User {} was not in the cache. Fetching details from LDAP.", userId);
        try {
            Map<String, Object> userAttrs = getAttributes(new UserId().id(userId));
            return new User().userId(new UserId().id(userId))
                    .auths(getAuths(new UserId().id(userId), userAttrs, context))
                    .roles(getRoles(new UserId().id(userId), userAttrs, context));
        } catch (NamingException ex) {
            LOGGER.error("Unable to get user from LDAP ", ex);
            throw new NoSuchUserIdException("Unable to get user from LDAP", ex);
        }
    }

    @Override
    public User addUser(final User user) {
        LOGGER.error("Adding users is not supported in this user service: {}", getClass().getSimpleName());
        throw new UnsupportedOperationException("Adding users is not supported in this user service: " + getClass().getSimpleName());
    }

    /**
     * Returns an array of attribute names to be fetched from LDAP. This could contain
     * user auth attributes and user role attributes. This will avoid having
     * to make multiple calls to LDAP to look up different attributes.
     * If this returns null, or an empty array then the LDAP request will not be made.
     *
     * @return the attributes to be fetched from LDAP.
     */
    protected abstract String[] getAttributeNames();

    /**
     * <p>
     * Gets the user auths from LDAP.
     * </p>
     * <p>
     * If possible the user auths should be extracted from the userAttrs parameter rather than making another call to LDAP.
     * </p>
     *
     * @param userId    the user ID
     * @param userAttrs the user attributes fetched from LDAP. This is populated with the attributes listed from {@link #getAttributeNames()}
     * @param context   the {@link LdapContext} for querying LDAP if required
     * @return the {@link Set} of user auths
     * @throws NamingException if a naming exception is encountered whilst interacting with LDAP
     */
    protected abstract Set<String> getAuths(final UserId userId, final Map<String, Object> userAttrs, final LdapContext context) throws NamingException;

    /**
     * <p>
     * Gets the user roles from LDAP.
     * </p>
     * <p>
     * If possible the user roles should be extracted from the userAttrs parameter rather than making another call to LDAP.
     * </p>
     *
     * @param userId    the user ID
     * @param userAttrs the user attributes fetched from LDAP. This is populated with the attributes listed from {@link #getAttributeNames()}
     * @param context   the {@link LdapContext} for querying LDAP if required
     * @return the {@link Set} of user roles
     * @throws NamingException if a naming exception is encountered whilst interacting with LDAP
     */
    protected abstract Set<String> getRoles(final UserId userId, final Map<String, Object> userAttrs, final LdapContext context) throws NamingException;

    protected LdapContext createContext(final String ldapConfigPath) throws IOException, NamingException {
        final Properties config = new Properties();
        if (new File(ldapConfigPath).exists()) {
            config.load(Files.newInputStream(Paths.get(ldapConfigPath)));
        } else {
            config.load(getClass().getResourceAsStream(ldapConfigPath));
        }
        return new InitialLdapContext(config, null);
    }

    protected Map<String, Object> getAttributes(final UserId userId) throws NamingException {
        final Map<String, Object> attributes = new HashMap<>();
        final String[] requestAttrs = getAttributeNames();
        if (null != requestAttrs && requestAttrs.length > 0) {
            final Attributes userAttrs = context.getAttributes(formatInput(userId.getId()), requestAttrs);
            if (null != userAttrs) {
                for (final String requestAttr : requestAttrs) {
                    final Attribute attribute = userAttrs.get(requestAttr);
                    if (null != attribute) {
                        final NamingEnumeration<?> all = attribute.getAll();
                        if (all.hasMore()) {
                            attributes.put(requestAttr, all.next());
                        }
                    }
                }
            }
        }
        return attributes;
    }

    /**
     * Performs a basic search on LDAP using the userId.
     *
     * @param userId          the userId to search for
     * @param name            the name of the context to search
     * @param attrIdForUserId the attribute ID that is associated with the userId
     * @param attrs           the attributes to fetch from the LDAP search.
     * @return the attribute values
     * @throws NamingException if a naming exception is encountered
     */
    protected Set<Object> basicSearch(final UserId userId,
                                      final String name, final String attrIdForUserId,
                                      final String... attrs) throws NamingException {
        LOGGER.debug("Performing basic search using {}, {}, {}, {}", userId, name, attrIdForUserId, attrs);
        final NamingEnumeration<SearchResult> attrResults = context.search(
                name,
                new BasicAttributes(attrIdForUserId, formatInput(userId.getId())),
                attrs
        );

        final Set<Object> results = new HashSet<>();
        while (attrResults.hasMore()) {
            final SearchResult result = attrResults.next();
            final Attributes resultAttrs = result.getAttributes();
            if (null != resultAttrs) {
                final NamingEnumeration<? extends Attribute> all = resultAttrs.getAll();
                if (null != all) {
                    while (all.hasMore()) {
                        final Attribute next = all.next();
                        final Object nextValue = next.get();
                        if (null != nextValue) {
                            results.add(nextValue);
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Formats an input string before being sent to LDAP. Essentially it just
     * escapes some characters.
     * Override this method to add additional formatting.
     *
     * @param input the input string to be formatted.
     * @return the formatted string
     */
    protected String formatInput(final String input) {
        LOGGER.debug("Formatting input with {} as the variable", input);
        String result = input;
        for (final String escapedChar : ESCAPED_CHARS) {
            result = result.replace(escapedChar, "\\" + escapedChar);
        }
        LOGGER.debug("Returning formatted input as {}", result);
        return result;
    }
}
