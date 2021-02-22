#Security17/18
#Grupo sc010

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
	
server: javac PhotoShareServer.java (compilar)
        java PhotoShareServer
        
cliente: javac PhotoShare.java (compilar)
         java PhotoShare <localUserId> <password> <127.0.0.1:23232>


Para a realização de uma operação é necessário:
	1ºCorrer o PhotoShareServer como indicado acima;
	2ºCorrer o PhotoShare como indicado acima;
	3º Após a autenticação ou criação de user novo:
		3.1- Indicar a operação fazendo - (a,i,g,l,f,r,L,D,c);
		3.2- Seguido dos argumentos necessarios para cada operacao.
	
	Exemplo de cada comando :
		
		Sendo userId- um user válido e a fculLogo.jpg apenas um nome exemplo e adoro seguranca um 			comentario de exemplo também.

		-a: -a fculLogo.jpeg
		-i: -i userId fculLogo.jpg
		-g: -g userId
		-l: -l userId
		-f: -f userId 
		-r: -r userId 
		-L: -L userId fculLogo.jpg
		-D: -L userId fculLogo.jpg
		-c: -c adoro segurança userId fculLogo.jpg