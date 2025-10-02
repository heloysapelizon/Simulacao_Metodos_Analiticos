# Simulador de Filas

## Como Executar

```bash
javac *.java    
java Main       
```

## Classes

- **`Main.java`** - Carrega YAML e executa simulação
- **`ConfigLoader.java`** - Lê arquivos de configuração YAML
- **`Simulador.java`** - Motor da simulação por eventos discretos
- **`Fila.java`** - Modelo de fila com estatísticas
- **`Evento.java`** - Eventos do sistema (chegada/saída/passagem)
- **`LCG.java`** - Gerador de números aleatórios

## Verificações das Especificações

**Para executar a validação (use arquivo Tandem.yml):**
```bash
# Edite Main.java: trocar "Hospital.yml" por "Tandem.yml"
javac Main.java
java Main
```

Edite os arquivos YAML para alterar:
- **Filas**: servidores, capacidade, tempos de atendimento
- **Roteamento**: probabilidades entre filas
- **Simulação**: seed, número de aleatórios
