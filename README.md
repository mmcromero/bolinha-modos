# Bolinha de modos

App Android com uma bolinha flutuante para ligar e desligar "modos" de conversa com o Claude.

Ao ligar um modo, o app copia o prompt completo para a área de transferência. Ao desligar, copia o comando curto de desativar. Você cola na conversa.

## Versão 0.1

- Bolinha flutuante e arrastável, com marca verde quando há modo ligado
- Interruptor por modo e botão "Desligar todos"
- Criar, editar e excluir modos dentro do app
- Exportar e importar modos em arquivo JSON
- Modos iniciais: Modo celular e Manual de aviação
- Entrada somente por Copiar (a entrada Automático fica para uma versão futura)

## Instalar o APK

1. Abra a aba Releases deste repositório no navegador do celular.
2. Baixe o arquivo `bolinha-modos.apk` do build mais recente.
3. Permita a instalação de apps de fonte desconhecida quando o Android pedir.
4. Abra o app, toque em "Mostrar bolinha" e permita "Exibir sobre outros apps".

## Build

O GitHub Actions compila o APK a cada envio para a branch `main` e publica em Releases.
Também dá para rodar manualmente em Actions > Build APK > Run workflow.

## Distribuição

APK direto durante o desenvolvimento e os testes. A intenção é publicar na Google Play depois de testado e aprovado.
