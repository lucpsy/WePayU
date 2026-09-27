private class RegistroHistorico {
    String estadoAnterior;
    Comando comando;
    RegistroHistorico(String estadoAnterior, Comando comando) {
        this.estadoAnterior = estadoAnterior;
        this.comando = comando;
    }
}
