package ecom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class ECommerceApp {
    static Scanner sc = new Scanner(System.in);
    static List<Product> products = new ArrayList<>();
    static List<Customer> customers = new ArrayList<>();
    static List<Order> orders = new ArrayList<>();
    static List<Payment> payments = new ArrayList<>();
    static List<Shipment> shipments = new ArrayList<>();
    static List<ReturnRequest> returns = new ArrayList<>();

    static int prodSeq = 1, custSeq = 1, orderSeq = 1, paySeq = 1, shipSeq = 1, retSeq = 1;
    static final int RETURN_WINDOW_DAYS = 30;

    public static void main(String[] args) {
        boolean run = true;
        while (run) {
            showMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": addProduct(); break;
                case "2": addCustomer(); break;
                case "3": placeOrder(); break;
                case "4": makePayment(); break;
                case "5": shipOrder(); break;
                case "6": requestReturn(); break;
                case "7": displayProducts(); break;
                case "8": run = false; System.out.println("Exiting. Bye!"); break;
                default: System.out.println("Invalid option, try again.");
            }
        }
    }

    static void showMenu(){
        System.out.println("\n=== E-Commerce Console ===");
        System.out.println("1. Add Product");
        System.out.println("2. Add Customer");
        System.out.println("3. Place Order");
        System.out.println("4. Make Payment");
        System.out.println("5. Ship Order");
        System.out.println("6. Request Return");
        System.out.println("7. Display Products");
        System.out.println("8. Exit");
        System.out.print("Choose: ");
    }

   
    static void addProduct(){
        System.out.print("Product name: ");
        String name = sc.nextLine();
        System.out.print("Price: ");
        double price = Double.parseDouble(sc.nextLine());
        System.out.print("Stock qty: ");
        int stock = Integer.parseInt(sc.nextLine());
        Product p = new Product(prodSeq++, name, price, stock);
        products.add(p);
        System.out.println("Added: " + p);
    }

 
    static void addCustomer(){
        System.out.print("Customer name: ");
        String name = sc.nextLine();
        System.out.print("Email: ");
        String email = sc.nextLine();
        Customer c = new Customer(custSeq++, name, email);
        customers.add(c);
        System.out.println("Added: " + c);
    }

 
    static void placeOrder(){
        if (customers.isEmpty() || products.isEmpty()) { System.out.println("Need at least one customer and one product."); return; }
        System.out.println("Select customer id:");
        customers.forEach(System.out::println);
        int cid = Integer.parseInt(sc.nextLine());
        Optional<Customer> cust = customers.stream().filter(c -> c.getId()==cid).findFirst();
        if (!cust.isPresent()){ System.out.println("Customer not found."); return; }

        Order o = new Order(orderSeq++, cust.get());
        boolean adding = true;
        while (adding) {
            System.out.println("Available products:");
            products.forEach(System.out::println);
            System.out.print("Enter product id to add (0 to finish): ");
            int pid = Integer.parseInt(sc.nextLine());
            if (pid==0) break;
            Optional<Product> pp = products.stream().filter(p->p.getId()==pid).findFirst();
            if (!pp.isPresent()){ System.out.println("Product not found"); continue; }
            System.out.print("Qty: "); int q = Integer.parseInt(sc.nextLine());
            if (q<=0){ System.out.println("Qty must be >0"); continue; }
  
            if (pp.get().getStock() < q) { System.out.println("Insufficient stock for product " + pp.get().getName()); continue; }
            o.addItem(new OrderItem(pp.get(), q));
            System.out.println("Added to order: " + pp.get().getName() + " x"+q);
        }
        if (o.getItems().isEmpty()){ System.out.println("No items added. Order cancelled."); return; }
    
        boolean confirmed = o.confirmOrder();
        if (!confirmed){ System.out.println("Order confirmation failed due to stock change."); return; }
        orders.add(o);
        System.out.println("Order placed. Invoice:\n" + o.invoice());
    }

   
    static void makePayment(){
        if (orders.isEmpty()){ System.out.println("No orders found."); return; }
        System.out.println("Orders:");
        orders.forEach(System.out::println);
        System.out.print("Enter order id to pay: ");
        int oid = Integer.parseInt(sc.nextLine());
        Optional<Order> oo = orders.stream().filter(o->o.getId()==oid).findFirst();
        if (!oo.isPresent()){ System.out.println("Order not found."); return; }
        Order o = oo.get();
        if (o.getStatus()!=OrderStatus.PLACED){ System.out.println("Only PLACED orders can be paid. Current: " + o.getStatus()); return; }
        System.out.println("Order total: " + o.totalAmount());
        System.out.print("Enter payment type (card/upi): ");
        String t = sc.nextLine().trim().toLowerCase();
        Payment pay;
        if (t.equals("card")) pay = new CardPayment(paySeq++, o, o.totalAmount());
        else pay = new GenericPayment(paySeq++, o, o.totalAmount());
        boolean ok = pay.process();
        payments.add(pay);
        if (ok){
            o.setStatus(OrderStatus.PAID);
            System.out.println("Payment successful. Payment id: " + pay.getId());
        } else {
            System.out.println("Payment failed.");
        }
    }

   
    static void shipOrder(){
        System.out.println("Orders:"); orders.forEach(System.out::println);
        System.out.print("Enter order id to ship: ");
        int oid = Integer.parseInt(sc.nextLine());
        Optional<Order> oo = orders.stream().filter(o->o.getId()==oid).findFirst();
        if (!oo.isPresent()){ System.out.println("Order not found."); return; }
        Order o = oo.get();
        if (o.getStatus()!=OrderStatus.PAID){ System.out.println("Order must be PAID before shipment. Current: " + o.getStatus()); return; }
        Shipment s = new Shipment(shipSeq++, o);
        boolean ok = s.ship();
        shipments.add(s);
        if (ok){
            o.setStatus(OrderStatus.SHIPPED);
            System.out.println("Order shipped. Shipment id: " + s.getId());
        } else System.out.println("Shipment failed.");
    }

    static void requestReturn(){
        if (orders.isEmpty()){ System.out.println("No orders."); return; }
        System.out.println("Orders:"); orders.forEach(System.out::println);
        System.out.print("Order id to return from: "); int oid = Integer.parseInt(sc.nextLine());
        Optional<Order> oo = orders.stream().filter(o->o.getId()==oid).findFirst();
        if (!oo.isPresent()){ System.out.println("Order not found."); return; }
        Order o = oo.get();
        System.out.println("Enter the product id to return (0 to finish):");
        Map<Integer,Integer> retmap = new HashMap<>();
        while (true){
            System.out.print("Product id (0 to end): "); int pid = Integer.parseInt(sc.nextLine()); if (pid==0) break;
            Optional<OrderItem> oi = o.getItems().stream().filter(it->it.getProduct().getId()==pid).findFirst();
            if (!oi.isPresent()){ System.out.println("Product not in this order."); continue; }
            System.out.print("Qty to return: "); int q = Integer.parseInt(sc.nextLine());
            if (q<=0 || q>oi.get().getQuantity()){ System.out.println("Invalid qty. Must be 1.."+oi.get().getQuantity()); continue; }
            retmap.put(pid, q);
        }
        if (retmap.isEmpty()){ System.out.println("No return items entered."); return; }
        ReturnRequest rr = new ReturnRequest(retSeq++, o, LocalDate.now());
     
        for (Map.Entry<Integer,Integer> e: retmap.entrySet()){
            Product p = products.stream().filter(pr->pr.getId()==e.getKey()).findFirst().get();
            rr.addReturnItem(p, e.getValue());
        }
        boolean approved = rr.evaluate();
        returns.add(rr);
        if (approved){
            rr.setStatus(ReturnStatus.APPROVED);
          
            rr.getReturnItems().forEach((prod,qty)-> prod.increaseStock(qty));
            System.out.println("Return approved. Stock updated.");
        } else {
            rr.setStatus(ReturnStatus.REJECTED);
            System.out.println("Return rejected.");
        }
    }

    static void displayProducts(){
        if (products.isEmpty()){ System.out.println("No products."); return; }
        products.forEach(System.out::println);
    }
}

class Product {
    private int id;
    private String name;
    private double price;
    private int stock;

    public Product(int id, String name, double price, int stock){ this.id=id; this.name=name; this.price=price; this.stock=stock; }

    public int getId(){ return id; }
    public String getName(){ return name; }
    public double getPrice(){ return price; }
    public int getStock(){ return stock; }

    public synchronized boolean reduceStock(int qty){
        if (qty<=0) return false;
        if (stock>=qty){ stock-=qty; return true; }
        return false;
    }
    public synchronized void increaseStock(int qty){ if (qty>0) stock+=qty; }

    @Override
    public String toString(){ return String.format("[%d] %s - Rs %.2f (stock=%d)", id, name, price, stock); }
}

class Customer {
    private int id; private String name; private String email;
    public Customer(int id, String name, String email){ this.id=id; this.name=name; this.email=email; }
    public int getId(){ return id; }
    public String getName(){ return name; }
    public String getEmail(){ return email; }
    @Override public String toString(){ return String.format("%d: %s <%s>", id, name, email); }
}

enum OrderStatus { PLACED, PAID, SHIPPED, COMPLETED, CANCELLED }

class Order {
    private int id;
    private Customer customer;
    private List<OrderItem> items = new ArrayList<>();
    private LocalDate orderDate;
    private OrderStatus status = OrderStatus.PLACED;

    public Order(int id, Customer customer){ this.id = id; this.customer = customer; this.orderDate = LocalDate.now(); }
    public int getId(){ return id; }
    public Customer getCustomer(){ return customer; }
    public List<OrderItem> getItems(){ return items; }
    public LocalDate getOrderDate(){ return orderDate; }
    public OrderStatus getStatus(){ return status; }
    public void setStatus(OrderStatus s){ this.status = s; }

    public void addItem(OrderItem i){ items.add(i); }

 
    public boolean confirmOrder(){
        for (OrderItem oi: items){ if (oi.getProduct().getStock() < oi.getQuantity()) return false; }
       
        for (OrderItem oi: items){ oi.getProduct().reduceStock(oi.getQuantity()); }
        return true;
    }

    public double totalAmount(){ double t=0; for (OrderItem oi: items) t+=oi.getLineTotal(); return t; }

    public String invoice(){
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice - Order #"+id+"\n");
        sb.append("Customer: " + customer.getName()+"\n");
        sb.append("Date: "+orderDate+"\n");
        for (OrderItem oi: items) sb.append(String.format("%s x%d = Rs %.2f\n", oi.getProduct().getName(), oi.getQuantity(), oi.getLineTotal()));
        sb.append(String.format("Total = Rs %.2f\n", totalAmount()));
        sb.append("Status: "+status+"\n");
        return sb.toString();
    }

    @Override public String toString(){ return String.format("Order[%d] Customer:%s Total:Rs %.2f Status:%s Date:%s", id, customer.getName(), totalAmount(), status, orderDate); }
}

class OrderItem {
    private Product product;
    private int quantity;
    public OrderItem(Product product, int quantity){ this.product=product; this.quantity=quantity; }
    public Product getProduct(){ return product; }
    public int getQuantity(){ return quantity; }
    public double getLineTotal(){ return product.getPrice() * quantity; }
}

enum PaymentStatus { PENDING, SUCCESS, FAILED }

abstract class Payment {
    protected int id;
    protected Order order;
    protected double amount;
    protected PaymentStatus status = PaymentStatus.PENDING;
    public Payment(int id, Order order, double amount){ this.id=id; this.order=order; this.amount=amount; }
    public int getId(){ return id; }
    public PaymentStatus getStatus(){ return status; }
    public abstract boolean process();
}

class GenericPayment extends Payment {
    public GenericPayment(int id, Order order, double amount){ super(id, order, amount); }
    @Override
    public boolean process(){
        if (amount<=0) { status = PaymentStatus.FAILED; return false; }
        status = PaymentStatus.SUCCESS; return true;
    }
}

class CardPayment extends Payment {
    public CardPayment(int id, Order order, double amount){ super(id, order, amount); }
    @Override
    public boolean process(){
        if (amount <= 0) { status = PaymentStatus.FAILED; return false; }

        status = PaymentStatus.SUCCESS; return true;
    }
}

enum ShipmentStatus { PENDING, SHIPPED, DELIVERED }

class Shipment {
    private int id; private Order order; private ShipmentStatus status = ShipmentStatus.PENDING; private LocalDate shippedDate;
    public Shipment(int id, Order order){ this.id=id; this.order=order; }
    public int getId(){ return id; }
    public boolean ship(){
        if (order.getStatus() != OrderStatus.PAID) return false;
        status = ShipmentStatus.SHIPPED; shippedDate = LocalDate.now();
        return true;
    }
}

enum ReturnStatus { REQUESTED, APPROVED, REJECTED }

class ReturnRequest {
    private int id; private Order order; private LocalDate requestDate; private ReturnStatus status = ReturnStatus.REQUESTED;
    
    private Map<Product, Integer> returnItems = new HashMap<>();
    public ReturnRequest(int id, Order order, LocalDate requestDate){ this.id=id; this.order=order; this.requestDate=requestDate; }
    public int getId(){ return id; }
    public void addReturnItem(Product p, int qty){ returnItems.put(p, qty); }
    public Map<Product,Integer> getReturnItems(){ return returnItems; }
    public ReturnStatus getStatus(){ return status; }
    public void setStatus(ReturnStatus s){ this.status = s; }

    public boolean evaluate(){
       
        long days = ChronoUnit.DAYS.between(order.getOrderDate(), requestDate);
        if (days > ECommerceApp.RETURN_WINDOW_DAYS) return false;
      
        for (Map.Entry<Product,Integer> e: returnItems.entrySet()){
            Product p = e.getKey(); int q = e.getValue();
            Optional<OrderItem> oi = order.getItems().stream().filter(it->it.getProduct().getId()==p.getId()).findFirst();
            if (!oi.isPresent()) return false;
            if (q > oi.get().getQuantity()) return false;
        }
        return true;
    }
}


