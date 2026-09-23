package com.iortatechnxt.finverse.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Fluent helper for full-stack API tests acting as a named demo user. */
@Component
public class Api {

  private final MockMvc mvc;
  private final UserDetailsService users;
  private final ObjectMapper json;

  Api(MockMvc mvc, UserDetailsService users, ObjectMapper json) {
    this.mvc = mvc;
    this.users = users;
    this.json = json;
  }

  public ResultActions doGet(String username, String url) throws Exception {
    return mvc.perform(as(username, get(url)));
  }

  public ResultActions doPost(String username, String url, Object body) throws Exception {
    return mvc.perform(withBody(as(username, post(url)), body));
  }

  public ResultActions doPut(String username, String url, Object body) throws Exception {
    return mvc.perform(withBody(as(username, put(url)), body));
  }

  public JsonNode read(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private MockHttpServletRequestBuilder as(String username, MockHttpServletRequestBuilder b) {
    return b.with(user(users.loadUserByUsername(username))).with(csrf());
  }

  private MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder b, Object body)
      throws Exception {
    return b.contentType(MediaType.APPLICATION_JSON)
        .content(
            body instanceof String s ? s : json.writeValueAsString(body == null ? "{}" : body));
  }
}
