package mindustry.logic;

public class LogicHandler {
    private Map<String, Object> variables = new HashMap<>();

    public Object eval(String expr) {
        if (expr == null) return null;
        if (expr.startsWith("$")) {
            String varName = expr.substring(1);
            return variables.getOrDefault(varName, "undefined");
        } else if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            return Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]);
        } else {
            return Integer.parseInt(expr);
        }
    }
}