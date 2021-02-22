## Seguranca e Confiabilidade Grupo 10
### Instrucoes
1. Setup da keystore
	1. Fazer input dos seguintes comandos :
		```java
		keytool -genkeypair -alias myServer -keyAlg RSA -keysize 2048 -keystore server
		keytool -exportcert -alias myServer -file certServer.cer -keystore server
		keytool -importcert -alias myServer -file certServer.cer -keystore client
		```
2. Correr Man Users
	1. Entrar no manusers
		1a. Se primeira vez que corre, escolher a password de admin
	2. Escrever password de admin
	3. Executar comandos desejados ( add, update, delete)
	4. Executar o comando quit quando terminado
3. Correr PhotoShare Server
	1. Introduzir a password de admin escolhida anteriormente
4. Correr PhotoShare Client
	1. Executar comandos desejados relativos a primeira etapa do projeto