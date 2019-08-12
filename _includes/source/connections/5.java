ConnectionConfig.builder()
  .url("ldap://directory.ldaptive.org")
  .useStartTLS(true)
  .connectTimeout(Duration.ofSeconds(5))
  .responseTimeout(Duration.ofSeconds(5))
  .sslConfig(SslConfig.builder()
    .credentialConfig(X509CredentialConfig.builder()
      .trustCertificates("file:/tmp/certs.pem")
      .authenticationCertificate("file:/tmp/mycert.pem")
      .authenticationKey("file:/tmp/mykey.pkcs8")
      .build())
    .build())
  .build()