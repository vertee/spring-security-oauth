package org.springframework.security.oauth2.provider.approval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.BaseClientDetails;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.InMemoryClientDetailsService;

public class TestApprovalStoreUserApprovalHandler {

	private ApprovalStoreUserApprovalHandler handler = new ApprovalStoreUserApprovalHandler();

	private InMemoryApprovalStore store = new InMemoryApprovalStore();

	private Authentication userAuthentication;

	@Before
	public void init() {
		handler.setApprovalStore(store);
		InMemoryClientDetailsService clientDetailsService = new InMemoryClientDetailsService();
		Map<String, ClientDetails> map = new HashMap<String, ClientDetails>();
		map.put("client", new BaseClientDetails("client", null, "read,write", "authorization_code", null));
		clientDetailsService.setClientDetailsStore(map);
		handler.setRequestFactory(new DefaultOAuth2RequestFactory(clientDetailsService));
		userAuthentication = new UsernamePasswordAuthenticationToken("user", "N/A",
				AuthorityUtils.commaSeparatedStringToAuthorityList("USER"));
	}

	@Test
	public void testExplicitlyApprovedScopes() {
		AuthorizationRequest authorizationRequest = new AuthorizationRequest("client", Arrays.asList("read"));
		authorizationRequest.setApprovalParameters(Collections.singletonMap("scope.read", "approved"));
		assertTrue(handler.isApproved(authorizationRequest, userAuthentication));
		assertEquals(1, store.getApprovals("user", "client").size());
		assertEquals(1, authorizationRequest.getScope().size());
		assertTrue(authorizationRequest.isApproved());
	}

	@Test
	public void testImplicitlyDeniedScope() {
		AuthorizationRequest authorizationRequest = new AuthorizationRequest("client", Arrays.asList("read", "write"));
		authorizationRequest.setApprovalParameters(Collections.singletonMap("scope.read", "approved"));
		assertTrue(handler.isApproved(authorizationRequest, userAuthentication));
		Collection<Approval> approvals = store.getApprovals("user", "client");
		assertEquals(2, approvals.size());
		approvals.contains(new Approval("user", "client", "read", new Date(), Approval.ApprovalStatus.APPROVED));
		approvals.contains(new Approval("user", "client", "write", new Date(), Approval.ApprovalStatus.DENIED));
		assertEquals(1, authorizationRequest.getScope().size());
	}

	@Test
	public void testExplicitlyPreapprovedScopes() {
		store.addApprovals(Arrays.asList(new Approval("user", "client", "read", new Date(System.currentTimeMillis() + 10000), Approval.ApprovalStatus.APPROVED)));
		AuthorizationRequest authorizationRequest = new AuthorizationRequest("client", Arrays.asList("read"));
		AuthorizationRequest result = handler.checkForPreApproval(authorizationRequest, userAuthentication);
		assertTrue(result.isApproved());
	}

	@Test
	public void testExpiredPreapprovedScopes() {
		store.addApprovals(Arrays.asList(new Approval("user", "client", "read", new Date(System.currentTimeMillis() - 10000), Approval.ApprovalStatus.APPROVED)));
		AuthorizationRequest authorizationRequest = new AuthorizationRequest("client", Arrays.asList("read"));
		AuthorizationRequest result = handler.checkForPreApproval(authorizationRequest, userAuthentication);
		assertFalse(result.isApproved());
	}

}
