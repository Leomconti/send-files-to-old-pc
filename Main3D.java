public class Main3D {
    // ... (existing code)
    
    ArrayList<Object3D> listaObjetos = new ArrayList<>();
    ArrayList<Enemy> listaInimigos = new ArrayList<>();
    
    // ... (existing code)
    
    private void init() {
        // ... (existing initialization code)
        
        // Create enemies
        createEnemies();
    }
    
    private void createEnemies() {
        ObjModel enemyModel = new ObjModel();
        enemyModel.loadObj("Mig_29_obj.obj");
        enemyModel.load();
        
        for (int i = 0; i < 5; i++) {
            Enemy enemy = new Enemy(
                rnd.nextFloat() * 20 - 10,
                rnd.nextFloat() * 10 + 5,
                rnd.nextFloat() * 20 - 10,
                0.01f
            );
            enemy.model = enemyModel;
            listaInimigos.add(enemy);
        }
    }
    
    private void gameUpdate(long diftime) {
        // ... (existing update code)
        
        // Update enemies
        for (Enemy enemy : listaInimigos) {
            enemy.SimulaSe(diftime);
        }
        
        // Check for collisions between projectiles and enemies
        for (int i = 0; i < listaObjetos.size(); i++) {
            Object3D obj = listaObjetos.get(i);
            if (obj instanceof Projetil) {
                Projetil projetil = (Projetil) obj;
                for (int j = 0; j < listaInimigos.size(); j++) {
                    Enemy enemy = listaInimigos.get(j);
                    if (checkCollision(projetil, enemy)) {
                        projetil.vivo = false;
                        listaInimigos.remove(j);
                        j--;
                        break;
                    }
                }
            }
        }
        
        // Remove dead objects
        listaObjetos.removeIf(obj -> !obj.vivo);
    }
    
    private boolean checkCollision(Object3D obj1, Object3D obj2) {
        float dx = obj1.x - obj2.x;
        float dy = obj1.y - obj2.y;
        float dz = obj1.z - obj2.z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance < (obj1.raio + obj2.raio);
    }
    
    private void gameRender() {
        // ... (existing render code)
        
        // Render enemies
        glBindTexture(GL_TEXTURE_2D, Constantes.txtmig);
        for (Enemy enemy : listaInimigos) {
            enemy.DesenhaSe(shader);
        }
        
        // ... (rest of the render code)
    }
}
