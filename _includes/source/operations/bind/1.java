SingleConnectionFactory cf = new SingleConnectionFactory("ldap://directory.ldaptive.org");
cf.initialize();
BindOperation bind = new BindOperation(cf);
BindResponse res = bind.execute(SimpleBindRequest.builder()
  .dn("uid=dfisher,ou=people,dc=ldaptive,dc=org")
  .password("password")
  .build());
if (res.isSuccess()) {
  // bind succeeded
} else {
  // bind failed
}
// use the connection factory to perform operations as uid=dfisher
cf.close();
