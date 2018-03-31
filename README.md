<h1>Jogo da forca em sistema distribuído</h1>
esse projeto foi primeiro trabalho para a disciplina de sistemas 
distribuidos UFMS-CPPP
<h2>Funcionamento</h2>

Primeiramente o cliente escaneia a rede a procura de servidores para poder jogar,
se encontrar, ele irá listar eles e o número de letras na maior e menor palavra no dicionário
, o tópico destas palavras e o número de vidas para cada servidor.

Quando o cliente seleciona um servidor ele atribui um id para o cliente e
retorna o tamanho da palavra escolhida para ele,
depois o cliente tenta adivinhar uma letra e o servidor retorna um vetor com as posições
que essa letra está na palavra, se o cliente errar a letra, ele vai perdendo suas vidas,
repete isso até a palavra ser descoberta ou suas vidas esgotarem, assim o jogo acaba
e o cliente poderá continuar requisitando outra palavra ou parar, quando há escravos conectados
no servidor o próprio separa o dicionário para esses escravos, assim quando um cliente conectar
ele irá distribuir a carga (escolher uma palavra, verificar tentativa, contar pontos) associando
a id do cliente com um dos escravos conectados.

<h2>Pré-requerimentos</h2>
<ul>
    <li>jre e jdk >=8</li>
    <li>make</li>
</ul>
<h2>Compilar</h2>
Basta navegar a pasta raíz e executar
<code>make</code>
<h2>Executar</h2>

<li><h3>Servidor</h3></li>
<code>java hangman.Server [ip da maquina] [caminho para arquivo de dicionario] [topico] [número de vidas]</code>

<li><h3>Cliente</h3></li>
<code>java hangman.Client</code>

<li><h3>Escravo</h3></li>
<code>java hangman.Slave [ip do servidor]</code>
