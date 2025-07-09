## Notas de Desenvolvimento

### Configuração do Agente Mockito

Para garantir compatibilidade futura com as novas versões do JDK e resolver o aviso "Mockito is currently self-attaching", o `maven-surefire-plugin` foi configurado no `pom.xml` para incluir o agente Mockito. Isso garante que o Mockito seja carregado como um agente Java durante a execução dos testes.

### Stubbing de Métodos de Teste

Erros `Cannot resolve method 'thenReturn(Attribute)'` e `Cannot resolve method 'thenReturn(String)'` em `DnsLookupServiceTest.java` foram resolvidos por:
*   Usar a sintaxe `doReturn().when()` para stubbing de `mockAllAttrs.next()`.
*   Realizar um cast explícito dos valores de retorno para `Object` ao stubbing de `mockAttrValuesA.next()` e `mockAttrValuesMX.next()` para lidar com problemas de inferência de tipo com genéricos `NamingEnumeration<?>`.