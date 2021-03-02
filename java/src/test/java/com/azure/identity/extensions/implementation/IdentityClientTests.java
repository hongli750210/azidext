// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.identity.extensions.implementation;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.util.logging.ClientLogger;
import com.azure.identity.extensions.util.TestUtils;
import com.azure.identity.implementation.IdentityClientOptions;
import com.azure.identity.implementation.util.CertificateUtil;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IClientCredential;
import com.microsoft.aad.msal4j.MsalServiceException;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.OnBehalfOfParameters;
import com.microsoft.aad.msal4j.UserAssertion;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "javax.net.ssl.*", "org.xml.*"})
@PrepareForTest({CertificateUtil.class, ClientCredentialFactory.class, Runtime.class, URL.class, ConfidentialClientApplication.class, ConfidentialClientApplication.Builder.class, PublicClientApplication.class, PublicClientApplication.Builder.class, IdentityClient.class})
public class IdentityClientTests {


    private static final  String CLIENT_ID = "<your-webapi(A)-client-id>";
    //    private String CLIENT_ID = "ea6caa78-2403-4f95-9aea-94cf799fa946";
    private static final  String TENANT_ID = "<your-webapi(A)-tenant-id>";
    //    private String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";
    private static final  String CLIENT_SECRET = "<your-webapi(A)-secret>";
    //    private String CLIENT_SECRET = "f58jr4yDcH-fvo-7etf6h.to3-q05Y_0l9";
    private static final  String SCOPE_URL = "<your-webapi(B)-scope>";
    //    private String SCOPE_URL = "https://graph.microsoft.com/.default";
    private static final  String ON_BEHALF_TOKEN = "<on-behalf-token-by-webapi(A)>";
    //    private String ON_BEHALF_TOKEN  = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Im5PbzNaRHJPRFhFSzFqS1doWHNsSFJfS1hFZyIsImtpZCI6Im5PbzNaRHJPRFhFSzFqS1doWHNsSFJfS1hFZyJ9.eyJhdWQiOiJodHRwczovL21hbmFnZW1lbnQuY29yZS53aW5kb3dzLm5ldC8iLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwiaWF0IjoxNjE0MjM5OTYxLCJuYmYiOjE2MTQyMzk5NjEsImV4cCI6MTYxNDI0Mzg2MSwiYWNyIjoiMSIsImFpbyI6IkFYUUFpLzhUQUFBQVR2R1RJZmV4aU5ETHFuNjlNMnVvQit1MnkreVRSU3Q5K3NNejU1WlpLREtieEIvV0VqaEJuamY1S3pkRXZBUFU5aC9IZjI3cXBKSGdwSmtRd3FBd2NPOXlDTS9KRDJuUnluWE5hM2hIMmcrMGk5YlNmSEN2NDVkaHBNTytPcGphRVpTV0kyaHNxMDhkYTlKbzFlU3Z2UT09IiwiYW1yIjpbInB3ZCIsInJzYSIsIm1mYSJdLCJhcHBpZCI6ImM0NGI0MDgzLTNiYjAtNDljMS1iNDdkLTk3NGU1M2NiZGYzYyIsImFwcGlkYWNyIjoiMiIsImRldmljZWlkIjoiOWQzNGYyNWYtOWZjNS00ZTgyLWE3OGEtZTdiOTFhYzgwNWIyIiwiZmFtaWx5X25hbWUiOiJMaSIsImdpdmVuX25hbWUiOiJIb25nIiwiZ3JvdXBzIjpbIjEwYTI3ODYwLWY4MzUtNDRiMC05YTNhLTIzMGQ1YzM4ZWZhNiIsIjU4ZmVkMDliLTM2ZGYtNGY1MS05MzE2LTIwM2U4ZWRiMWNkNSIsIjk0NzdhMzhkLTY0ZTYtNDA4Yi05YjY4LTAzMTIzZTU3YmVlZiIsIjAzZGUzMzJlLWFiMmYtNGUxNC1iNTU4LTllM2E2NDhiNGY4NCIsIjliYWM4MGU2LTdkYWMtNGIyZC05MTNiLTJmNmRhODA5M2MwMSIsIjU0MTUzYmM4LTQ5NDMtNDI2ZC1iMDM4LTg4ZTMyNGI2Y2RiMCIsIjJiMjZhYmY1LTNhNjktNDg3MS1iOTNmLWI1MjEwMDkzMmMwOCIsIjQwODBmYWY5LTMxNjQtNDdmZS04MzE3LTRkMjgxMTAxMzkzZCIsImQyYTI2YzkxLTYyY2EtNGFlZS04ZTczLTNmYzk1YmIwNTQ1NyIsIjE1MTY4OWM0LTBjOTEtNGUyMi04NmU4LWZlOTVlYzU0N2Y0NCIsIjAzNTNhNTAzLWNiOTAtNDAxYS1iN2ZjLTA2YjY1ZTFhNDJiNiIsIjA5NGY5YTU1LTQ0ZDAtNDMxNy04MDBiLWU2NzlhNDYwMjliNiIsIjE1ZDUyOTg4LWE3NmMtNDhjYS04ZTRjLTZkN2I0NDMyMWUzMiIsIjZjOTc2NGMxLWZhZTEtNDI5Ni05MTQ0LWZmMzkwODU2MTk2NCIsImIwMWMxNGE0LWU3NzQtNGQ3Yi1hZTM2LWQ1MjdjYTY1MGVmYyIsImU3NmFiMzFlLWRkNTgtNDBlMi1hN2MxLTNlYmIwMjkyMTIxMSIsIjRhYzg1MzRkLWRlZDctNDlhZC04ZTYxLTU3M2FmOTUyMmZhZiIsImE5ZmE5MjU4LWVjN2UtNDc4My04ZmEyLTFmNzk2NjlhYWU0NCIsIjA1ODdjYmI5LTE4ZWItNDdiNC1iYWU4LTUzN2EyMWViM2M2NiIsImQxYzA1MGIwLTE3MGItNGNjZi1iMWMxLWE0NDA5MTU2NzYxOSIsIjU2ZjBjNzViLTVjMTMtNDVjOS1hZDJlLTY0ZGRhMjkyOGM4MyIsImE3MTAyNjhkLTUxYTktNDQ0Ny1iZjIwLTVjNDI4ZmQ3MDQ3YyIsImU0NzRlNzEyLWFjMjEtNGRkOS05M2Y2LTQzYjBiNmYwNWJlNCIsIjc1MmYxOTVhLWYzZDktNDI0Mi05Y2FjLWQ0NTlmODMzYmZkNSIsImYxNzM3YjQxLTUyYzItNDhjYy1hNjRmLTljNmY3YTc2NWYxZiIsIjkxNDllMDBkLTBmYWUtNDMxYy1iNzJlLTY3YzA0MmFlMmQzNSIsIjNkZTFiYTAzLTU1ZTgtNGFhNC1iZTUzLWJkNjFmZDNmMjA2MCIsIjdiNTNjNWI3LTgzZDEtNDFjMC05ZmFlLWY3NGJhYWNmMzcxOCIsImMzZDcxOTFjLTNkNjItNDI4OC1iZTQwLTU1MDAzODM4OTBkZiIsImQxNWJjN2UxLTc1ZmEtNDVlMi1hYjgxLTI0MDY0YmYyYzY5NiIsIjk4NmY0YTI4LWJmMGEtNGRmZi05NDEyLWE0MjVjNDIzOWIyYyIsImRmNGVkYTE1LTZiZjctNDNkYy04ZGZmLTdmODhlMDRjMTc0NSIsImY0NDQ0ZGFlLTRiY2UtNDAyZi1iNTRiLWYxMjQ2MWEzNmJiYyIsIjAyY2Q2ZjFmLTMwMjktNDIzOC05ZmRjLWIwZjI5YjY4NGY3NSIsIjlkNTI5ZDVlLWI4ODItNGZmYi1hOGU2LWM0MTQ3YzY4MDczNSIsImJlOGNhMzc4LWJjNzQtNDZjMS1iOTIyLWU3ZjU1MjQ4NmVkZSIsIjdlOGZhMzJjLTk4NDEtNGQwOC1iODkzLTM0MWM3NWE5OWNhMiIsIjNjMjQ0NmFlLTliNmMtNDlhYS04ZmIxLTU2ZmE5NjUzYmQ5YiIsImU3YzQzZDAwLTQyMDAtNDRmZS04YmRiLWQ0OGZjNDg0MWUyYSIsIjBkODY0Njc2LTdmNTUtNDY5NC05MGYyLTViNzVlYjc3YjM0MiIsIjNjNGZjZTQ2LTUzYzktNDlmNy05YmNjLWU5OTdkYzU2OGMwOSIsIjU1ZWZhOWM5LTkyNDYtNDhiZi05N2E0LTk5ODc5NzdjNGQ1ZCIsIjgzNjYxMjY1LWUzOTQtNGM1Ni04YmQ0LTZkYmU3OWRhNGZmMCIsIjJhYjY3ZWYzLTEyMDEtNDk5NS04ZTk0LWMxYzQyYWY3ZDdhYiIsIjE3OTY3ZDIxLWIzMGQtNDliYS1hOTg3LThlMmNhMjRjYmMzMiIsImI2MTI3Mzg2LTEwOTQtNDdkMC05MTk3LTJiNzgzNmE5MDJiZSIsIjU1MzM0NzI3LWYzZTQtNGE4OC1iNmUyLWZjMTZmYWU2ZjQyNSIsIjFlZGJlMDBjLTA3ZjgtNDFjMS04YzBjLThiYTU4ZjNhZGZmZSIsIjhmNDBhZjU1LTU2MTctNDNkMC04MDUyLTI0NDg4ZWRkOWQ2NiIsIjkyYjEwZGY3LTBjZGUtNGE0MC1hNmVjLTUwMWVlMTM1YjI5NyIsIjdhZTIyMGQ5LWQ4ZmMtNGIwZC04ODk2LTAwMDc2YTQ4YWE0ZiIsIjI3ZGZmMDBmLWMzMDgtNDMxZS1hZmVlLWNhOGRmODQ3NWY0YiIsIjYyZWRiZDdiLThkNDYtNGQyYy1hNWExLWRhNWI3OGJhMWQzOCIsIjFkYzJmMmMwLWE0YjItNGM4OS05YzEzLWZiNmIwZGFkNTM2MCJdLCJpcGFkZHIiOiIyNy4xMTUuNjkuMjAiLCJuYW1lIjoiSG9uZyBMaSAoV0lDUkVTT0ZUIE5PUlRIIEFNRVJJQ0EgTFREKSIsIm9pZCI6IjMzZTNmYThhLTg0ZDAtNDJkYy04NjFmLWVhZDA3NWEzNjU1YSIsIm9ucHJlbV9zaWQiOiJTLTEtNS0yMS0yMTQ2NzczMDg1LTkwMzM2MzI4NS03MTkzNDQ3MDctMjY4NTQxNSIsInB1aWQiOiIxMDAzMjAwMEY3NjhGREJDIiwicmgiOiIwLkFSb0F2NGo1Y3ZHR3IwR1JxeTE4MEJIYlI0TkFTOFN3TzhGSnRIMlhUbFBMM3p3YUFHSS4iLCJzY3AiOiJ1c2VyX2ltcGVyc29uYXRpb24iLCJzdWIiOiJ5Mk1JejNOY2hOM25XZnlrN1o0NmJMaU9DbmFEY3NjT1pWUG8ydWUtLVowIiwidGlkIjoiNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3IiwidW5pcXVlX25hbWUiOiJ2LWhvbmdsaTFAbWljcm9zb2Z0LmNvbSIsInVwbiI6InYtaG9uZ2xpMUBtaWNyb3NvZnQuY29tIiwidXRpIjoibUQydVVwNnZQazJXbG5saExZRmJBQSIsInZlciI6IjEuMCIsInhtc190Y2R0IjoxMjg5MjQxNTQ3fQ.c2dZmc288SxdZTY8hqhoM47ihLSz_18sZvr39ZDVEwqZcF_hgfE98wd3P3Q7CVTDkdQ78oNoUUgD7gKY3uGhTdXQZqjhI0KR2Yd1IITV-Od57iByNNStckmGBtRNUeBvx_I2E3dZjXvSQ_HJRl_PBVV9d9CfGRXQ1PFB29CHVHGMOmVU0BY8VgPKkll9T8dH-8D5StPSIjqcSGuPWnLieGmbNyFCEixlwqzTdaEUxQkU1vaDnJQmF-QIcRJnCjnb6uGslIgwroA5_teQ4athiVJUmbeGraaNAy1gaRfLFkXDzY14hmB74FLanikL8-TMrnPnNr5kIjYs6VvKAFcBRw";

    private final ClientLogger logger = new ClientLogger(IdentityClientTests.class);

    @Test
    public void testValidOnBehalfOfFLowCredential() throws Exception {
        // setup
        UserAssertion userAssertion = new UserAssertion(ON_BEHALF_TOKEN );
        TokenRequestContext request = new TokenRequestContext().addScopes(SCOPE_URL);
        OffsetDateTime expiresOn = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        // mock
        mockForOnBehalfOfFlowCredential(ON_BEHALF_TOKEN , request, expiresOn);

        // test
        IdentityClientOptions options = new IdentityClientOptions();
        IdentityClient client = new IdentityClientBuilder().tenantId(TENANT_ID).clientId(CLIENT_ID).clientSecret(CLIENT_SECRET).identityClientOptions(options).build();
        AccessToken token = client.authenticateWithOnBehalfOfCredential(request, userAssertion).block();
        Assert.assertEquals(ON_BEHALF_TOKEN , token.getToken());
        Assert.assertEquals(expiresOn.getSecond(), token.getExpiresAt().getSecond());
    }

    private void mockForOnBehalfOfFlowCredential(String token, TokenRequestContext request, OffsetDateTime expiresAt) throws Exception {
        ConfidentialClientApplication application = PowerMockito.mock(ConfidentialClientApplication.class);
        when(application.acquireToken(any(OnBehalfOfParameters.class))).thenAnswer(invocation -> {
            OnBehalfOfParameters argument = (OnBehalfOfParameters) invocation.getArguments()[0];
            if (argument.scopes().size() == 1 && request.getScopes().get(0).equals(argument.scopes().iterator().next())) {
                return TestUtils.getMockAuthenticationResult(token, expiresAt);
            } else {
                return CompletableFuture.runAsync(() -> {
                    throw new MsalServiceException("Invalid request", "InvalidScopes");
                });
            }
        });
        ConfidentialClientApplication.Builder builder = PowerMockito.mock(ConfidentialClientApplication.Builder.class);
        when(builder.build()).thenReturn(application);
        when(builder.authority(any())).thenReturn(builder);
        when(builder.httpClient(any())).thenReturn(builder);
        whenNew(ConfidentialClientApplication.Builder.class).withAnyArguments().thenAnswer(invocation -> {
            String cid = (String) invocation.getArguments()[0];
            IClientCredential keyCredential = (IClientCredential) invocation.getArguments()[1];
            if (!CLIENT_ID.equals(cid)) {
                throw new MsalServiceException("Invalid CLIENT_ID", "InvalidClientId");
            }
            if (keyCredential == null) {
                throw new MsalServiceException("Invalid clientCertificate", "InvalidClientCertificate");
            }
            return builder;
        });
    }

}
