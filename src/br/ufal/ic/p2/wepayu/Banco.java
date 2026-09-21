package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Empregado;

import java.time.LocalDate;
import java.util.*;

public class Banco {
    private int instancia = 1;
    SequencedMap<String, Empregado> empregados = new LinkedHashMap<>();
    Map<String, Map<String, String>> bancoDeHoras = new LinkedHashMap<>();
    public void clear(){
        empregados.clear();
        bancoDeHoras.clear();
    }
    public void add(Empregado e){
        String id = "id" + instancia;
        instancia++;
        empregados.put(id,e);
        bancoDeHoras.put(id,new LinkedHashMap<>());
    }

}
