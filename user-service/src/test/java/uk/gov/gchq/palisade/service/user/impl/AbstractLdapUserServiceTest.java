/*
 * Copyright 2020 Crown Copyright
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractLdapUserServiceTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(AbstractLdapUserService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> getMessages(final Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    public void shouldFetchUserDetailsFromLdap() throws NamingException {
        // Given
        final AbstractLdapUserService mock = mock(AbstractLdapUserService.class);
        final UserId userId = new UserId().id("user#01");
        final LdapContext context = mock(LdapContext.class);

        final String[] attrNames = {"roles", "auths"};
        final Set<String> auths = Sets.newHashSet("auth1", "auth2");
        final Set<String> roles = Sets.newHashSet("role1", "role2");

        final Attributes requestAttrs = new BasicAttributes();
        requestAttrs.put("auths", auths);
        requestAttrs.put("roles", roles);

        final Map<String, Object> userAttrs = new HashMap<>();
        userAttrs.put("auths", auths);
        userAttrs.put("roles", roles);

        given(mock.getAttributeNames()).willReturn(attrNames);
        given(context.getAttributes("user\\#01", attrNames)).willReturn(requestAttrs);
        given(mock.getAuths(userId, userAttrs, context)).willReturn(auths);
        given(mock.getRoles(userId, userAttrs, context)).willReturn(roles);

        final MockLdapUserService service = new MockLdapUserService(context);
        service.setMock(mock);

        // When
        final User user = service.getUser(userId);

        // Then
        assertEquals(userId, user.getUserId());
        assertEquals(auths, user.getAuths());
        assertEquals(roles, user.getRoles());

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("was not in the cache. Fetching details from LDAP")
        ));
    }


    @Test
    public void shouldPerformABasicSearch() throws NamingException {
        // Given
        final AbstractLdapUserService mock = mock(AbstractLdapUserService.class);
        final UserId userId = new UserId().id("user01");
        final LdapContext context = mock(LdapContext.class);

        final MockLdapUserService service = new MockLdapUserService(context);
        final String searchBase = "base";
        final String attrIdForUserId = "userId";
        final String[] requestAttrs = {"attr1", "attr2"};
        service.setMock(mock);

        final Attributes searchResult1Attrs = new BasicAttributes();
        final Set<String> search1Attr1 = Sets.newHashSet("auth1", "auth2");
        final int search1Attr2 = 10;
        searchResult1Attrs.put("search1Attr1", search1Attr1);
        searchResult1Attrs.put("search1Attr2", search1Attr2);

        final Attributes searchResult2Attrs = new BasicAttributes();
        final Long search2Att1 = 5L;
        final String search2Attr2 = "search2Attr2";
        searchResult2Attrs.put("search2Att1", search2Att1);
        searchResult2Attrs.put("search2Attr2", search2Attr2);

        final SearchResult searchResult1 = new SearchResult("key1", "value1", searchResult1Attrs);
        final SearchResult searchResult2 = new SearchResult("key2", "value2", searchResult2Attrs);

        Iterator<SearchResult> itr = Arrays.asList(searchResult1, searchResult2).iterator();
        final NamingEnumeration<SearchResult> responseAttrs = new NamingEnumeration<SearchResult>() {
            @Override
            public SearchResult next() throws NamingException {
                return itr.next();
            }

            @Override
            public boolean hasMore() throws NamingException {
                return itr.hasNext();
            }

            @Override
            public void close() throws NamingException {
            }

            @Override
            public boolean hasMoreElements() {
                return itr.hasNext();
            }

            @Override
            public SearchResult nextElement() {
                return itr.next();
            }
        };

        given(context.search(searchBase,
                new BasicAttributes(attrIdForUserId, userId.getId()),
                requestAttrs)
        ).willReturn(responseAttrs);

        // When
        final Set<Object> results = service.basicSearch(userId, searchBase, attrIdForUserId, requestAttrs);

        // Then
        verify(context, times(1)).search(searchBase,
                new BasicAttributes(attrIdForUserId, userId.getId()),
                requestAttrs);
        final Set<Object> expectedResults = Sets.newHashSet(search1Attr1, search1Attr2, search2Att1, search2Attr2);
        assertEquals(expectedResults, results);

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Performing basic search using")
        ));
    }

    @Test
    public void shouldEscapeCharacters() throws NamingException {
        // Given
        final AbstractLdapUserService mock = mock(AbstractLdapUserService.class);
        final LdapContext context = mock(LdapContext.class);

        final MockLdapUserService service = new MockLdapUserService(context);
        service.setMock(mock);

        final String input = "test input: " + Stream.of(AbstractLdapUserService.ESCAPED_CHARS)
                .collect(Collectors.joining());

        // When
        final String result = service.formatInput(input);

        // Then
        final String expectedResult = "test input: " + Stream.of(AbstractLdapUserService.ESCAPED_CHARS)
                .map(t -> "\\" + t)
                .collect(Collectors.joining());
        assertEquals(expectedResult, result);
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Formatting input with"),
                Matchers.containsString("Returning formatted input as")
        ));
    }

    public static final class MockLdapUserService extends AbstractLdapUserService {
        private AbstractLdapUserService mock;

        public MockLdapUserService(final LdapContext context) {
            super(context);
        }

        public MockLdapUserService(@JsonProperty("ldapConfigPath") final String ldapConfigPath) throws IOException, NamingException {
            super(ldapConfigPath);
        }

        @Override
        protected String[] getAttributeNames() {
            return mock.getAttributeNames();
        }

        @Override
        protected Set<String> getAuths(final UserId userId, final Map<String, Object> userAttrs, final LdapContext context) throws NamingException {
            return mock.getAuths(userId, userAttrs, context);
        }

        @Override
        protected Set<String> getRoles(final UserId userId, final Map<String, Object> userAttrs, final LdapContext context) throws NamingException {
            return mock.getRoles(userId, userAttrs, context);
        }

        public void setMock(final AbstractLdapUserService mock) {
            this.mock = mock;
        }
    }
}
