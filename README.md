# EasyBindings

EasyBindings is a simple Java framework to create Cocoa based key-value bindings. It uses JavaFX properties to listen for changes.

You should clone it in `{java_project_root}/src/org/panther`.

## Example

```java
import org.panther.easybindings.EasyBinding;

class House {
    public StringProperty name = new SimpleStringProperty();
}

class Family {
    public ObjectProperty<House> house = new SimpleObjectProperty();
}

static public class Main {
    public static void main(String... args) {
        ObjectProperty<Family> a = new SimpleObjectProperty(new Family());
        a.getValue().house.getValue().name.setValue("Bob's Ranch");
        ObjectProperty<House> b = new SimpleObjectProperty(new House());
        
        EasyBinding binding = new EasyBinding(b, "root.name", a, "root.house.name");
        binding.bind();
        
        System.out.println(b.getValue().name.getValue()); // -> Bob's Ranch
        
        House house = new House()
        house.name.setValue("Evy's Ranch");
        a.getValue().house.setValue(house);
        System.out.println(b.getValue().name.getValue()); // -> Evy's Ranch
        
        // bindings are bidirectional (for now)
        b.getValue().house.getValue().name.setValue("Jackie's Ranch");
        System.out.println(a.getValue().name.getValue()); // -> Jackie's Ranch
        
        binding.unbind();
        
        a.getValue().house.getValue().name.setValue("Garbage");
        System.out.println(b.getValue().name.getValue()); // -> Jackie's Ranch
    }
}
```
