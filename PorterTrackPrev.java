package CaseStudy;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PorterTrackPrev {

    /* ================= ANSI COLORS ================= */
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    /* ---------------- Base classes ---------------- */
    static class Person {
        protected String name;
        protected String id;

        public Person(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public String getName() { return name; }
        public String getId() { return id; }
    }

    static class UniversityMember extends Person {
        private String role;
        private String pin; // optional simple PIN for member if they want login later

        public UniversityMember(String name, String id, String role) {
            super(name, id);
            this.role = (role == null || role.isEmpty()) ? "Member" : role;
        }

        public String getRole() { return role; }
        public void setPin(String pin) { this.pin = pin; }
        public String getPin() { return pin; }

        @Override
        public String toString() {
            return name + " (" + role + ", ID: " + id + ")";
        }
    }

    static class Porter extends Person {
        private boolean available = true;
        private final List<Integer> ratings = new ArrayList<>();
        private String pin; // simple PIN for porter login

        public Porter(String name, String id, String pin) {
            super(name, id);
            this.pin = pin;
        }

        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public void addRating(int r) { if (r >= 1 && r <= 5) ratings.add(r); }
        public double getAverageRating() {
            if (ratings.isEmpty()) return 0.0;
            int sum = 0;
            for (int r : ratings) sum += r;
            return (double) sum / ratings.size();
        }
        public int getRatingsCount() { return ratings.size(); }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }

        @Override
        public String toString() {
            String avail = available ? GREEN + "Available" + RESET : RED + "Busy" + RESET;
            String avg = ratings.isEmpty() ? "No ratings yet" : String.format("%.2f/5 (%d)", getAverageRating(), ratings.size());
            return "🧍 Porter: " + name + " (ID: " + id + ") - " + avail + " | Avg: " + avg;
        }
    }

    /* ---------------- Delivery & Enums ---------------- */
    enum Status { REQUESTED, ASSIGNED, OUT_FOR_DELIVERY, DELIVERED, NOT_DELIVERED, COMPLETED }
    enum Priority { HIGH, MEDIUM, LOW, UNKNOWN }
    enum Category { DOCUMENTS, ELECTRONICS, FOOD, LAB_EQUIPMENT, OTHER }

    static class Delivery {
        private static int counter = 1;

        private final int deliveryId;
        private final UniversityMember sender;
        private final String receiverName, receiverPhone, receiverAddress;
        private final String item;
        private final Priority priority;
        private final Category category;
        private Status status;
        private Porter assignedPorter;
        private Integer rating;
        private String review;
        private LocalDateTime requestedAt;
        private LocalDateTime assignedAt, outForDeliveryAt, deliveredAt, notDeliveredAt, completedAt;
        private int estimatedMinutes; // ETA estimation

        public Delivery(UniversityMember sender,
                        String receiverName, String receiverPhone, String receiverAddress,
                        String item, Priority priority, Category category) {
            this.deliveryId = counter++;  this.sender = sender; this.receiverName = receiverName;
            this.receiverPhone = receiverPhone; this.receiverAddress = receiverAddress; this.item = item;
            this.priority = priority == null ? Priority.UNKNOWN : priority;
            this.category = category == null ? Category.OTHER : category;
            this.status = Status.REQUESTED;  this.requestedAt = LocalDateTime.now();
            this.estimatedMinutes = estimateETA(priority);
        }

        private int estimateETA(Priority p) {
            if (p == null) return 30;
            switch (p) {
                case HIGH: return 10;
                case MEDIUM: return 20;
                case LOW: return 30;
                default: return 30;
            }
        }

        /* ---------------- Enhanced Status Display with Icons ---------------- */
        private String getStatusWithIcon(Status status) {
            switch (status) {
                case REQUESTED: return "🆕 " + status;
                case ASSIGNED: return "📌 " + status;
                case OUT_FOR_DELIVERY: return "🚴 " + status;
                case DELIVERED: return "✅ " + status;
                case NOT_DELIVERED: return "❌ " + status;
                case COMPLETED: return "🏁 " + status;
                default: return "❓ " + status;
            }
        }

        public int getDeliveryId() { return deliveryId; }
        public UniversityMember getSender() { return sender; }
        public String getReceiverName() { return receiverName; }
        public Status getStatus() { return status; }
        public Porter getAssignedPorter() { return assignedPorter; }
        public Integer getRating() { return rating; }
        public String getReview() { return review; }
        public Category getCategory() { return category; }
        public Priority getPriority() { return priority; }
        public int getEstimatedMinutes() { return estimatedMinutes; }

        public void assignPorter(Porter p) {
            this.assignedPorter = p;
            this.status = Status.ASSIGNED;
            this.assignedAt = LocalDateTime.now();
            if (p != null) p.setAvailable(false);
        }

        public void markOutForDelivery() {
            this.status = Status.OUT_FOR_DELIVERY;
            this.outForDeliveryAt = LocalDateTime.now();
        }

        public void markDelivered() {
            this.status = Status.DELIVERED;
            this.deliveredAt = LocalDateTime.now();
            if (assignedPorter != null) assignedPorter.setAvailable(true);
        }

        public void markNotDelivered() {
            this.status = Status.NOT_DELIVERED;
            this.notDeliveredAt = LocalDateTime.now();
            if (assignedPorter != null) assignedPorter.setAvailable(true);
        }

        public void markCompleted(Integer rating, String review) {
            this.status = Status.COMPLETED;
            this.completedAt = LocalDateTime.now();
            if (rating != null && rating >= 1 && rating <= 5) {
                this.rating = rating;
                if (assignedPorter != null) assignedPorter.addRating(rating);
            } else this.rating = null;
            this.review = (review == null || review.isEmpty()) ? null : review;
        }

        private String fmt(LocalDateTime dt) {
            if (dt == null) return "-";
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(BLUE).append("📦 Delivery #").append(deliveryId).append(RESET).append("\n");
            sb.append("   🧑 Sender: ").append(sender.getName()).append(" (").append(sender.getRole()).append(" | ID: ").append(sender.getId()).append(")\n");
            sb.append("   🏷 Receiver: ").append(receiverName).append(" | Phone: ").append(receiverPhone).append("\n");
            sb.append("   🏠 Address: ").append(receiverAddress).append("\n");
            sb.append("   📦 Item: ").append(item).append(" | Category: ").append(category).append(" | Priority: ").append(priority).append("\n");
            sb.append("   ⏱ ETA: ").append(estimatedMinutes).append(" minutes\n");
            sb.append("   🧍 Porter: ").append(assignedPorter != null ? assignedPorter.getName() + " (ID:" + assignedPorter.getId() + ")" : "Not Assigned").append("\n");
            sb.append("   📋 Status: ").append(getStatusWithIcon(status)).append("\n");
            sb.append("      🕒 Requested: ").append(fmt(requestedAt)).append(" -> ").append(getStatusWithIcon(Status.REQUESTED)).append("\n");
            if (assignedAt != null) sb.append("      📌 Assigned: ").append(fmt(assignedAt)).append(" -> ").append(getStatusWithIcon(Status.ASSIGNED)).append("\n");
            if (outForDeliveryAt != null) sb.append("      🚴 Out for Delivery: ").append(fmt(outForDeliveryAt)).append(" -> ").append(getStatusWithIcon(Status.OUT_FOR_DELIVERY)).append("\n");
            if (deliveredAt != null) sb.append("      ✅ Delivered: ").append(fmt(deliveredAt)).append(" -> ").append(getStatusWithIcon(Status.DELIVERED)).append("\n");
            if (notDeliveredAt != null) sb.append("      ❌ Not Delivered: ").append(fmt(notDeliveredAt)).append(" -> ").append(getStatusWithIcon(Status.NOT_DELIVERED)).append("\n");
            if (completedAt != null) {
                sb.append("      🏁 Completed: ").append(fmt(completedAt)).append(" -> ").append(getStatusWithIcon(Status.COMPLETED)).append("\n");
                if (rating != null) sb.append("         ⭐ Rating: ").append(rating).append("/5\n");
                if (review != null) sb.append("         📝 Review: ").append(review).append("\n");
            }
            return sb.toString();
        }

        // A minimal CSV representation for persistence
        public String toCSV() {
            // fields separated by | to avoid comma conflicts
            StringBuilder sb = new StringBuilder();
            sb.append(deliveryId).append("|");  sb.append(sender.getId()).append("|");
            sb.append(receiverName.replace("|", " ")).append("|"); sb.append(receiverPhone).append("|");
            sb.append(receiverAddress.replace("|", " ")).append("|");
            sb.append(item.replace("|", " ")).append("|"); sb.append(priority).append("|");
            sb.append(category).append("|"); sb.append(status).append("|");
            sb.append(assignedPorter == null ? "" : assignedPorter.getId()).append("|");
            sb.append(rating == null ? "" : rating).append("|");
            sb.append(review == null ? "" : review.replace("|", " ")).append("|");
            sb.append(requestedAt == null ? "" : requestedAt.toString()).append("|");
            sb.append(assignedAt == null ? "" : assignedAt.toString()).append("|");
            sb.append(outForDeliveryAt == null ? "" : outForDeliveryAt.toString()).append("|");
            sb.append(deliveredAt == null ? "" : deliveredAt.toString()).append("|");
            sb.append(notDeliveredAt == null ? "" : notDeliveredAt.toString()).append("|");
            sb.append(completedAt == null ? "" : completedAt.toString()).append("|");
            sb.append(estimatedMinutes);
            return sb.toString();
        }
    }

    /* ---------------- Controller: PorterManager ---------------- */
    static class PorterManager {
        private final List<Porter> porters = new ArrayList<>();
        private final List<Delivery> deliveries = new ArrayList<>();
        private final Queue<Delivery> pendingDeliveries = new LinkedList<>();
        private final Scanner sc = new Scanner(System.in);

        // Persistence toggle
        private final boolean persistenceEnabled = true;
        private final String PORTERS_FILE = "porters.csv";
        private final String DELIVERIES_FILE = "deliveries.csv";

        public void startSystem() {
            printBanner();
            if (persistenceEnabled) { loadPortersFromFile(); loadDeliveriesFromFile(); }
            while (true) {
                System.out.println();
                System.out.println(CYAN + "🔐 Choose Login Type:" + RESET);
                System.out.println("1️⃣  Admin");
                System.out.println("2️⃣  University Member (anyone)");
                System.out.println("3️⃣  Porter");
                System.out.println("0️⃣  Exit");
                System.out.print("👉 Enter choice: ");
                String choice = sc.nextLine().trim();

                switch (choice) {
                    case "1": adminLogin(); break;
                    case "2": memberLogin(); break;
                    case "3": porterLogin(); break;
                    case "0":
                        System.out.println(GREEN + "👋 Exiting PorterTrack... Goodbye!" + RESET);
                        if (persistenceEnabled) { savePortersToFile(); saveDeliveriesToFile(); }
                        return;
                    default:
                        System.out.println(RED + "❌ Invalid choice. Try again." + RESET);
                }
            }
        }

        private void printBanner() {
            System.out.println(MAGENTA + "==============================================");
            System.out.println("              PORTERTRACK    ");
            System.out.println("==============================================" + RESET);
        }

        /* ---------------- Admin ---------------- */
        private void adminLogin() {
            System.out.print("🔑 Enter Admin Password: ");
            String pass = sc.nextLine();
            if (!"admin123".equals(pass)) {
                System.out.println(RED + "❌ Access Denied!" + RESET);
                return;
            }
            while (true) {
                System.out.println();
                System.out.println(YELLOW + "+----------------------+");
                System.out.println("|     ADMIN PANEL      |");
                System.out.println("+----------------------+" + RESET);
                System.out.println("1️⃣  ➕ Add Porter");
                System.out.println("2️⃣  👀 View Porters");
                System.out.println("3️⃣  📋 View All Deliveries");
                System.out.println("4️⃣  📊 Dashboard (Stats)");
                // removed Update/Override Delivery Status per request
                System.out.println("5️⃣  🗑  Delete Porter");
                System.out.println("6️⃣  🗑  Delete Delivery");
                System.out.println("0️⃣  ↩  Back");
                System.out.print("👉 Choose: ");
                String ch = sc.nextLine().trim();

                switch (ch) {
                    case "1": addPorter(); break;
                    case "2": viewPorters(); break;
                    case "3": viewAllDeliveries(); break;
                    case "4": showDashboard(); break;
                    case "5": deletePorter(); break;
                    case "6": deleteDelivery(); break;
                    case "0": return;
                    default: System.out.println(RED + "❌ Invalid choice!" + RESET);
                }
            }
        }

        private void addPorter() {
            System.out.print("📝 Enter Porter Name: ");
            String name = sc.nextLine().trim();
            System.out.print("🆔 Enter Porter ID: ");
            String id = sc.nextLine().trim();
            if (name.isEmpty() || id.isEmpty()) {
                System.out.println(RED + "❌ Name or ID cannot be empty." + RESET);
                return;
            }
            if (findPorterById(id) != null) {
                System.out.println(YELLOW + "⚠ Porter ID already exists." + RESET);
                return;
            }
            System.out.print("🔒 Set a numeric PIN for porter (4 digits recommended): ");
            String pin = sc.nextLine().trim();
            porters.add(new Porter(name, id, pin));
            System.out.println(GREEN + "✅ Porter added successfully!" + RESET);
            if (persistenceEnabled) savePortersToFile();
            checkPendingDeliveries();
        }

        private void viewPorters() {
            if (porters.isEmpty()) {
                System.out.println(YELLOW + "⚠ No porters available." + RESET);
                return;
            }
            for (Porter p : porters) System.out.println(p);
        }

        private void viewAllDeliveries() {
            if (deliveries.isEmpty()) {
                System.out.println(YELLOW + "⚠ No deliveries yet." + RESET);
                return;
            }
            for (Delivery d : deliveries) System.out.println(d);
        }

        private void showDashboard() {
            int total = deliveries.size();
            int completed = 0, failed = 0, pending = 0, delivered = 0;
            double totalRatings = 0;
            int ratingCount = 0;
            for (Delivery d : deliveries) {
                switch (d.getStatus()) {
                    case COMPLETED: completed++; break;
                    case NOT_DELIVERED: failed++; break;
                    case DELIVERED: delivered++; break;
                    default: pending++;
                }
                if (d.getRating() != null) { totalRatings += d.getRating(); ratingCount++; }
            }
            System.out.println();
            System.out.println(CYAN + "📊 ADMIN DASHBOARD" + RESET);
            System.out.println("Total deliveries: " + total);
            System.out.println("Completed: " + completed + " | Delivered(not confirmed): " + delivered + " | Failed: " + failed + " | Pending/Assigned: " + pending);
            if (ratingCount > 0) System.out.println("Average Rating across deliveries: " + String.format("%.2f/5", totalRatings / ratingCount));
            else System.out.println("Average Rating: No ratings yet");
            System.out.println();
            System.out.println("Porter performance:");
            for (Porter p : porters) {
                System.out.println(" - " + p.getName() + " (ID:" + p.getId() + ") | Avg Rating: " + (p.getRatingsCount() == 0 ? "No ratings" : String.format("%.2f/5 (%d)", p.getAverageRating(), p.getRatingsCount())));
            }
        }

        /* ---------------- New Feature: Delete Porter ---------------- */
        private void deletePorter() {
            System.out.print("🆔 Enter Porter ID to delete: ");
            String id = sc.nextLine().trim();
            Porter porter = findPorterById(id);
            if (porter == null) {
                System.out.println(RED + "❌ Porter not found." + RESET);
                return;
            }

            // Check if porter has any active deliveries
            boolean hasActiveDeliveries = false;
            for (Delivery d : deliveries) {
                if (d.getAssignedPorter() != null && d.getAssignedPorter().getId().equals(id) &&
                        (d.getStatus() == Status.ASSIGNED || d.getStatus() == Status.OUT_FOR_DELIVERY)) {
                    hasActiveDeliveries = true;
                    break;
                }
            }

            if (hasActiveDeliveries) {
                System.out.println(RED + "❌ Cannot delete porter with active deliveries. Reassign deliveries first." + RESET);
                return;
            }

            System.out.print("⚠ Are you sure you want to delete porter " + porter.getName() + "? (yes/no): ");
            String confirm = sc.nextLine().trim().toLowerCase();
            if (confirm.equals("yes") || confirm.equals("y")) {
                porters.remove(porter);
                System.out.println(GREEN + "✅ Porter " + porter.getName() + " deleted successfully." + RESET);
                if (persistenceEnabled) savePortersToFile();
            } else {
                System.out.println(YELLOW + "⚠ Deletion cancelled." + RESET);
            }
        }

        /* ---------------- New Feature: Delete Delivery ---------------- */
        private void deleteDelivery() {
            System.out.print("📌 Enter Delivery ID to delete: ");
            int id = parseIntSafe(sc.nextLine());
            if (id < 0) {
                System.out.println(RED + "❌ Invalid ID." + RESET);
                return;
            }

            Delivery delivery = findDeliveryById(id);
            if (delivery == null) {
                System.out.println(RED + "❌ Delivery not found." + RESET);
                return;
            }

            // Only allow deletion of REQUESTED or COMPLETED deliveries
            if (delivery.getStatus() != Status.REQUESTED && delivery.getStatus() != Status.COMPLETED) {
                System.out.println(RED + "❌ Cannot delete delivery with status: " + delivery.getStatus() +
                        ". Only REQUESTED or COMPLETED deliveries can be deleted." + RESET);
                return;
            }

            System.out.println(delivery);
            System.out.print("⚠ Are you sure you want to delete this delivery? (yes/no): ");
            String confirm = sc.nextLine().trim().toLowerCase();
            if (confirm.equals("yes") || confirm.equals("y")) {
                deliveries.remove(delivery);
                // Also remove from pending queue if exists
                pendingDeliveries.remove(delivery);
                System.out.println(GREEN + "✅ Delivery #" + id + " deleted successfully." + RESET);
                if (persistenceEnabled) saveDeliveriesToFile();
            } else {
                System.out.println(YELLOW + "⚠ Deletion cancelled." + RESET);
            }
        }

        /* ---------------- Member ---------------- */
        private void memberLogin() {
            System.out.print("👤 Enter your name: ");
            String name = sc.nextLine().trim();
            System.out.print("🆔 Enter your ID (or press Enter to auto-generate): ");
            String id = sc.nextLine().trim();
            if (id.isEmpty()) id = "M" + (1000 + new Random().nextInt(9000));
            System.out.print("🏷 Enter your role (Student/Teacher/Staff): ");
            String role = sc.nextLine().trim();
            System.out.println(GREEN + "✅ Role set: " + (role.isEmpty() ? "Member" : role) + RESET);
            UniversityMember member = new UniversityMember(name, id, role);
            // removed PIN prompt per request

            while (true) {
                System.out.println();
                System.out.println(YELLOW + "+--------------------------------------------------+");
                System.out.println("|                🎓 UNIVERSITY MEMBER PANEL        |");
                System.out.println("+--------------------------------------------------+" + RESET);
                System.out.println("1️⃣  📦 Create Delivery Request");
                System.out.println("2️⃣  👀 View My Deliveries");
                System.out.println("3️⃣  ✅ Confirm Received & Rate");
                System.out.println("4️⃣  🧾 Print Delivery Receipt");
                System.out.println("0️⃣  ↩ Back");
                System.out.print("👉 Choose: ");
                String choice = sc.nextLine().trim();

                switch (choice) {
                    case "1": createDelivery(member); break;
                    case "2": viewMemberDeliveries(member); break;
                    case "3": confirmAndRate(member); break;
                    case "4": printReceipt(member); break;
                    case "0": return;
                    default: System.out.println(RED + "❌ Invalid choice!" + RESET);
                }
            }
        }

        private void createDelivery(UniversityMember sender) {
            System.out.print("📥 Enter Receiver Name: ");
            String rName = sc.nextLine().trim();
            System.out.print("📞 Enter Receiver Phone: ");
            String rPhone = sc.nextLine().trim();
            System.out.print("🏠 Enter Receiver Address: ");
            String rAddress = sc.nextLine().trim();
            System.out.print("📦 Enter Item Name: ");
            String item = sc.nextLine().trim();
            // removed category selection prompt per request; defaulting to OTHER
            Category cat = Category.OTHER;

            System.out.print("🎯 Enter Priority (High/Medium/Low): ");
            String pr = sc.nextLine().trim().toUpperCase();
            Priority priority;
            try { priority = Priority.valueOf(pr.isEmpty() ? "UNKNOWN" : pr); } catch (Exception e) { priority = Priority.UNKNOWN; }

            Delivery d = new Delivery(sender, rName, rPhone, rAddress, item, priority, cat);
            deliveries.add(d);
            System.out.println(GREEN + "✅ Delivery Request Created. ID: " + d.getDeliveryId() + " | ETA: " + d.getEstimatedMinutes() + " minutes" + RESET);
            assignPorterIfAvailable(d);
            System.out.println(d);
            if (persistenceEnabled) saveDeliveriesToFile();
        }

        private void viewMemberDeliveries(UniversityMember m) {
            boolean found = false;
            for (Delivery d : deliveries) {
                if (d.getSender().getId().equals(m.getId())) {
                    System.out.println(d);
                    found = true;
                }
            }
            if (!found) System.out.println(YELLOW + "⚠ No deliveries found for " + m.getName() + RESET);
        }

        private void confirmAndRate(UniversityMember m) {
            System.out.print("📌 Enter Delivery ID to confirm received: ");
            int id = parseIntSafe(sc.nextLine());
            if (id < 0) { System.out.println(RED + "❌ Invalid ID." + RESET); return; }
            Delivery d = findDeliveryById(id);
            if (d == null) { System.out.println(RED + "❌ Delivery not found." + RESET); return; }

            // Authorization: sender or receiver allowed
            if (!d.getSender().getId().equals(m.getId()) && !d.getReceiverName().equalsIgnoreCase(m.getName())) {
                System.out.println(RED + "❌ You are not authorized to confirm this delivery." + RESET);
                return;
            }

            if (d.getStatus() != Status.DELIVERED && d.getStatus() != Status.NOT_DELIVERED) {
                System.out.println(YELLOW + "⚠ Delivery not yet marked DELIVERED/NOT_DELIVERED by porter. Current status: " + d.getStatus() + RESET);
            }

            System.out.print("⭐ Provide rating (1-5) or press Enter to skip: ");
            Integer rating = parseIntSafeNullable(sc.nextLine());
            if (rating != null && (rating < 1 || rating > 5)) {
                System.out.println(YELLOW + "⚠ Invalid rating. It will be skipped." + RESET);
                rating = null;
            }
            System.out.print("📝 Write a short review (press Enter to skip): ");
            String rev = sc.nextLine().trim();
            d.markCompleted(rating, rev.isEmpty() ? null : rev);
            System.out.println(GREEN + "✅ Delivery #" + d.getDeliveryId() + " marked COMPLETED. Thank you for feedback!" + RESET);

            // print receipt automatically on completion
            printReceiptForDelivery(d);

            checkPendingDeliveries();
            if (persistenceEnabled) saveDeliveriesToFile();
        }

        private void printReceipt(UniversityMember m) {
            System.out.print("📌 Enter Delivery ID for receipt: ");
            int id = parseIntSafe(sc.nextLine());
            if (id < 0) { System.out.println(RED + "❌ Invalid ID." + RESET); return; }
            Delivery d = findDeliveryById(id);
            if (d == null) { System.out.println(RED + "❌ Delivery not found." + RESET); return; }
            // Only sender or receiver or admin should print
            if (!d.getSender().getId().equals(m.getId()) && !d.getReceiverName().equalsIgnoreCase(m.getName())) {
                System.out.println(RED + "❌ You are not authorized to print this receipt." + RESET);
                return;
            }
            printReceiptForDelivery(d);
        }

        private void printReceiptForDelivery(Delivery d) {
            System.out.println();
            System.out.println(MAGENTA + "==========================================" + RESET);
            System.out.println(MAGENTA + "               DELIVERY RECEIPT           " + RESET);
            System.out.println(MAGENTA + "==========================================" + RESET);
            System.out.println("Delivery ID: " + d.getDeliveryId());
            System.out.println("Sender: " + d.getSender().getName() + " (" + d.getSender().getId() + ")");
            System.out.println("Receiver: " + d.getReceiverName());
            System.out.println("Item: " + d.item);
            System.out.println("Category: " + d.getCategory());
            System.out.println("Priority: " + d.getPriority());
            System.out.println("Porter: " + (d.getAssignedPorter() == null ? "Not Assigned" : d.getAssignedPorter().getName() + " (ID:" + d.getAssignedPorter().getId() + ")"));
            System.out.println("Status: " + d.getStatus());
            if (d.getRating() != null) System.out.println("Rating: " + d.getRating() + "/5");
            if (d.getReview() != null) System.out.println("Review: " + d.getReview());
            System.out.println("Requested At: " + (d.requestedAt == null ? "-" : d.requestedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
            System.out.println(MAGENTA + "==========================================" + RESET);
        }

        /* ---------------- Porter ---------------- */
        private void porterLogin() {
            System.out.print("🧍 Enter your Porter ID: ");
            String id = sc.nextLine().trim();
            Porter porter = findPorterById(id);
            if (porter == null) {
                System.out.println(RED + "❌ Porter not found. Ask admin to add you." + RESET);
                return;
            }
            System.out.print("🔒 Enter your PIN: ");
            String pin = sc.nextLine().trim();
            if (porter.getPin() == null || !porter.getPin().equals(pin)) {
                System.out.println(RED + "❌ Incorrect PIN." + RESET);
                return;
            }
            System.out.println(GREEN + "✅ Welcome, " + porter.getName() + RESET);

            while (true) {
                System.out.println();
                System.out.println(YELLOW + "+----------------------+");
                System.out.println("|     PORTER PANEL     |");
                System.out.println("+----------------------+" + RESET);
                System.out.println("1️⃣  📋 View My Assigned Deliveries");
                System.out.println("2️⃣  🚴 Mark OUT_FOR_DELIVERY");
                System.out.println("3️⃣  ✅ Mark DELIVERED");
                System.out.println("4️⃣  ❌ Mark NOT_DELIVERED (failed attempt)");
                System.out.println("5️⃣  ⭐ View My Average Rating");
                System.out.println("6️⃣  ⏱  Update Delivery ETA");
                System.out.println("0️⃣  ↩ Back");
                System.out.print("👉 Choose: ");
                String ch = sc.nextLine().trim();

                switch (ch) {
                    case "1": viewPorterAssignedDeliveries(porter); break;
                    case "2": porterMarkOutForDelivery(porter); break;
                    case "3": porterMarkDelivered(porter); break;
                    case "4": porterMarkNotDelivered(porter); break;
                    case "5":
                        double avg = porter.getAverageRating();
                        if (avg == 0.0) System.out.println(YELLOW + "⚠ No ratings yet." + RESET);
                        else System.out.println(GREEN + "⭐ Your average rating: " + String.format("%.2f/5", avg) + RESET);
                        break;
                    case "6": updateDeliveryETA(porter); break;
                    case "0": return;
                    default: System.out.println(RED + "❌ Invalid choice!" + RESET);
                }
            }
        }

        private void viewPorterAssignedDeliveries(Porter p) {
            boolean found = false;
            for (Delivery d : deliveries) {
                if (d.getAssignedPorter() != null && d.getAssignedPorter().getId().equals(p.getId())) {
                    System.out.println(d);
                    found = true;
                }
            }
            if (!found) System.out.println(YELLOW + "⚠ No deliveries assigned to you currently." + RESET);
        }

        private void porterMarkOutForDelivery(Porter p) {
            System.out.print("📌 Enter Delivery ID to mark OUT_FOR_DELIVERY: ");
            int id = parseIntSafe(sc.nextLine());
            if (id < 0) { System.out.println(RED + "❌ Invalid ID." + RESET); return; }
            Delivery d = findDeliveryById(id);
            if (!isAssignedToPorter(d, p)) { System.out.println(RED + "❌ You cannot modify this delivery (not assigned to you)." + RESET); return; }
            d.markOutForDelivery();
            System.out.println(GREEN + "✅ Delivery #" + id + " marked OUT_FOR_DELIVERY." + RESET);
            if (persistenceEnabled) saveDeliveriesToFile();
        }

        private void porterMarkDelivered(Porter p) {
            System.out.print("📌 Enter Delivery ID to mark DELIVERED: ");
            int id = parseIntSafe(sc.nextLine());
            if (id < 0) { System.out.println(RED + "❌ Invalid ID." + RESET); return; }
            Delivery d = findDeliveryById(id);
            if (!isAssignedToPorter(d, p)) { System.out.println(RED + "❌ You cannot modify this delivery (not assigned to you)." + RESET); return; }
            d.markDelivered();
            System.out.println(GREEN + "✅ Delivery #" + id + " marked DELIVERED. You are now available." + RESET);
            // Improved message: receiver confirms rating
            System.out.println(CYAN + "🔔 Note: Receiver can confirm and rate this delivery." + RESET);
            checkPendingDeliveries();
            if (persistenceEnabled) saveDeliveriesToFile();
        }

        private void porterMarkNotDelivered(Porter p) {
            System.out.print("📌 Enter Delivery ID to mark NOT_DELIVERED: ");
            int id = parseIntSafe(sc.nextLine());
            if (id < 0) { System.out.println(RED + "❌ Invalid ID." + RESET); return; }
            Delivery d = findDeliveryById(id);
            if (!isAssignedToPorter(d, p)) { System.out.println(RED + "❌ You cannot modify this delivery (not assigned to you)." + RESET); return; }
            System.out.print("📝 Optional note (press Enter to skip): ");
            String note = sc.nextLine().trim();
            d.markNotDelivered();
            System.out.println(GREEN + "✅ Delivery #" + id + " marked NOT_DELIVERED. You are now available." + RESET);
            checkPendingDeliveries();
            if (persistenceEnabled) saveDeliveriesToFile();
        }

        /* ---------------- New Feature: Update Delivery ETA by Porter ---------------- */
        private void updateDeliveryETA(Porter porter) {
            System.out.print("📌 Enter Delivery ID to update ETA: ");
            int id = parseIntSafe(sc.nextLine());
            if (id < 0) {
                System.out.println(RED + "❌ Invalid ID." + RESET);
                return;
            }

            Delivery delivery = findDeliveryById(id);
            if (!isAssignedToPorter(delivery, porter)) {
                System.out.println(RED + "❌ You cannot update ETA for this delivery (not assigned to you)." + RESET);
                return;
            }

            System.out.println("Current ETA: " + delivery.getEstimatedMinutes() + " minutes");
            System.out.print("Enter new ETA in minutes: ");
            int newETA = parseIntSafe(sc.nextLine());
            if (newETA <= 0) {
                System.out.println(RED + "❌ ETA must be a positive number." + RESET);
                return;
            }

            // Update ETA using reflection since estimatedMinutes is private
            try {
                java.lang.reflect.Field etaField = Delivery.class.getDeclaredField("estimatedMinutes");
                etaField.setAccessible(true);
                etaField.set(delivery, newETA);
                System.out.println(GREEN + "✅ ETA updated to " + newETA + " minutes for Delivery #" + id + RESET);

                // Print updated delivery info
                System.out.println(CYAN + "📦 Updated Delivery Info:" + RESET);
                System.out.println("   🆔 Delivery #" + delivery.getDeliveryId());
                System.out.println("   📦 Item: " + delivery.item);
                System.out.println("   🎯 Priority: " + delivery.getPriority());
                System.out.println("   ⏱ New ETA: " + newETA + " minutes");
                System.out.println("   📋 Status: " + delivery.getStatus());

                if (persistenceEnabled) saveDeliveriesToFile();
            } catch (Exception e) {
                System.out.println(RED + "❌ Error updating ETA: " + e.getMessage() + RESET);
            }
        }

        /* ---------------- Helpers ---------------- */

        private void assignPorterIfAvailable(Delivery d) {
            for (Porter p : porters) {
                if (p.isAvailable()) {
                    d.assignPorter(p);
                    System.out.println(GREEN + "🚴 Assigned to Porter: " + p.getName() + " (ID: " + p.getId() + ")" + RESET);
                    if (persistenceEnabled) saveDeliveriesToFile();
                    return;
                }
            }
            pendingDeliveries.offer(d);
            System.out.println(YELLOW + "⏳ No available porters. Delivery added to pending queue." + RESET);
        }

        private void checkPendingDeliveries() {
            if (pendingDeliveries.isEmpty()) return;
            Iterator<Delivery> it = pendingDeliveries.iterator();
            while (it.hasNext()) {
                Delivery queued = it.next();
                boolean assigned = false;
                for (Porter p : porters) {
                    if (p.isAvailable()) {
                        queued.assignPorter(p);
                        System.out.println(GREEN + "✅ Pending Delivery #" + queued.getDeliveryId() + " auto-assigned to " + p.getName() + RESET);
                        it.remove();
                        assigned = true;
                        break;
                    }
                }
                if (!assigned) break; // no available porters currently
            }
            if (persistenceEnabled) saveDeliveriesToFile();
        }

        private Delivery findDeliveryById(int id) {
            for (Delivery d : deliveries) if (d.getDeliveryId() == id) return d;
            return null;
        }

        private Porter findPorterById(String id) {
            for (Porter p : porters) if (p.getId().equals(id)) return p;
            return null;
        }

        private boolean isAssignedToPorter(Delivery d, Porter p) {
            if (d == null || p == null) return false;
            return d.getAssignedPorter() != null && d.getAssignedPorter().getId().equals(p.getId());
        }

        private int parseIntSafe(String s) {
            try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
        }

        private Integer parseIntSafeNullable(String s) {
            try {
                if (s == null) return null;
                s = s.trim();
                if (s.isEmpty()) return null;
                return Integer.parseInt(s);
            } catch (Exception e) { return null; }
        }

        /* ---------------- Persistence (Simple CSV) ---------------- */

        private void savePortersToFile() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(PORTERS_FILE))) {
                for (Porter p : porters) {
                    pw.println(p.getId() + "|" + p.getName().replace("|", " ") + "|" + (p.getPin() == null ? "" : p.getPin()));
                }
            } catch (IOException e) {
                System.out.println(RED + "Error saving porters: " + e.getMessage() + RESET);
            }
        }

        private void loadPortersFromFile() {
            File f = new File(PORTERS_FILE);
            if (!f.exists()) return;
            try (Scanner reader = new Scanner(f)) {
                while (reader.hasNextLine()) {
                    String line = reader.nextLine().trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 2) continue;
                    String id = parts[0], name = parts[1];
                    String pin = parts.length >= 3 ? parts[2] : "";
                    Porter p = new Porter(name, id, pin);
                    porters.add(p);
                }
            } catch (IOException e) {
                System.out.println(RED + "Error loading porters: " + e.getMessage() + RESET);
            }
        }

        private void saveDeliveriesToFile() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(DELIVERIES_FILE))) {
                for (Delivery d : deliveries) {
                    pw.println(d.toCSV());
                }
            } catch (IOException e) {
                System.out.println(RED + "Error saving deliveries: " + e.getMessage() + RESET);
            }
        }

        private void loadDeliveriesFromFile() {
            File f = new File(DELIVERIES_FILE);
            if (!f.exists()) return;
            try (Scanner reader = new Scanner(f)) {
                while (reader.hasNextLine()) {
                    String line = reader.nextLine().trim();
                    if (line.isEmpty()) continue;
                    // parse using | separator (fields were written with toCSV())
                    String[] p = line.split("\\|", -1);

                    if (p.length < 13) continue;
                    String senderId = p[1];
                    UniversityMember sender = new UniversityMember("Unknown", senderId, "Member");
                    String receiverName = p[2], phone = p[3], address = p[4], item = p[5];
                    Priority priority;
                    try { priority = Priority.valueOf(p[6]); } catch (Exception e) { priority = Priority.UNKNOWN; }
                    Category category;
                    try { category = Category.valueOf(p[7]); } catch (Exception e) { category = Category.OTHER; }
                    Delivery d = new Delivery(sender, receiverName, phone, address, item, priority, category);

                    try {
                        Status s = Status.valueOf(p[8]);

                        switch (s) {
                            case ASSIGNED: d.status = Status.ASSIGNED; break;
                            case OUT_FOR_DELIVERY: d.status = Status.OUT_FOR_DELIVERY; break;
                            case DELIVERED: d.status = Status.DELIVERED; break;
                            case NOT_DELIVERED: d.status = Status.NOT_DELIVERED; break;
                            case COMPLETED: d.status = Status.COMPLETED; break;
                            default: d.status = Status.REQUESTED;
                        }
                    } catch (Exception ignore) {}

                    String porterId = p[9];
                    if (porterId != null && !porterId.isEmpty()) {
                        Porter porter = findPorterById(porterId);
                        if (porter == null) {
                            // create placeholder porter (will be updated if porters file contains real data)
                            porter = new Porter("Unknown", porterId, "");
                            porters.add(porter);
                        }
                        d.assignedPorter = porter;
                    }
                    String ratingStr = p[10];
                    if (ratingStr != null && !ratingStr.isEmpty()) {
                        try { d.rating = Integer.parseInt(ratingStr); } catch (Exception ignore) {}
                    }
                    String review = p[11];
                    if (review != null && !review.isEmpty()) d.review = review;

                    // Note: timestamps are not fully restored here; delivery objects restored with approximate status only
                    deliveries.add(d);
                }
            } catch (IOException e) {
                System.out.println(RED + "Error loading deliveries: " + e.getMessage() + RESET);
            }
        }
    }

    /* ---------------- Main ---------------- */
    public static void main(String[] args) {
        PorterManager manager = new PorterManager();
        manager.startSystem();
    }
}