package com.yjwdb2021.jumanji.config.oauth2;

import com.auth0.jwt.JWT;
import com.yjwdb2021.jumanji.config.jwt.JwtTokenUtil;
import com.yjwdb2021.jumanji.service.exception.ApiErrorResponse;
import com.yjwdb2021.jumanji.service.exception.BadRequestException;
import javassist.tools.web.BadHttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static com.yjwdb2021.jumanji.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private JwtTokenUtil jwtTokenUtil;
    private AppProperties appProperties;
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Autowired
    OAuth2AuthenticationSuccessHandler(JwtTokenUtil jwtTokenUtil, AppProperties appProperties, HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository){
        this.jwtTokenUtil = jwtTokenUtil;
        this.appProperties = appProperties;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException{
        String targetUrl = determineTargetUrl(request, response, authentication);

        if(response.isCommitted()){
            System.out.println("????????? ?????? ?????????????????????. " + targetUrl +  "??? ?????????????????? ??? ??? ????????????.");
            return;
        }
        System.out.println("?????? ????????????!");
        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication){
        System.out.println("OAtuh2AuthenticationSuccessHandler...");

        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if(redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())){
            throw new BadRequestException("???????????? ?????? ???????????? URI??? ?????? ????????? ????????? ??? ????????????.");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        System.out.println(
                "targetUrl : " + targetUrl +"\n" +
                        "authentication : " + authentication.toString() + "\n" +
                        "request : " + request.toString() + "\n" +
                        "response : " + response.toString()
        );
        String token = jwtTokenUtil.createToken(authentication);

        return UriComponentsBuilder.fromUriString(targetUrl) //TODO ????????????????????? ??????????????? ?????? Response body??? ????????????.
                .queryParam("token", token)
                .build().toUriString();
    }
    
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response){
        super.clearAuthenticationAttributes(request);

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
    
    private boolean isAuthorizedRedirectUri(String uri){
        URI clientRedirectUri = URI.create(uri);
        
        return appProperties.getOauth2().getAuthorizedRedirectUris()
                .stream()
                .anyMatch(authorizedRedirectUri -> {
                    URI authorizedURI = URI.create(authorizedRedirectUri);

                    if(authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost()) && authorizedURI.getPort() == clientRedirectUri.getPort()){
                        return true;
                    }
                    return false;
                });
    }

}
