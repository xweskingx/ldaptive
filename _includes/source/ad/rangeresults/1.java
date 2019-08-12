SearchOperation search = new SearchOperation(new DefaultConnectionFactory("ldap://directory.ldaptive.org"));
search.setSearchResultHandlers(new RangeEntryHandler());
SearchResponse res = search.execute(SearchRequest.builder()
  .dn("dc=ldaptive,dc=org")
  .filter("(sn=fisher)")
  .attributes("mail", "displayName")
  .build());
LdapEntry entry = res.getEntry(); // if you're expecting a single entry
for (LdapEntry le : res.getEntries()) { // if you're expecting multiple entries
  // do something useful with the entry
}