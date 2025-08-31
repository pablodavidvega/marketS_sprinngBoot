const SalesData= {
    labels:["Febrero", "Marzo","Agosto","Septiembre", "Diciembre"],
    datasets:[{
        label: "Ventas Grandes",
        data: [40000000, 60000000, 52000000, 72000000, 80000000],
        backgroundColor: "rgba(255, 0, 0,02)",
        borderColor: "rgba(0, 0, 0,1)",
        borderWidth: 1

    }]
};

const salesChart= new Chart(
    document.getElementById("LineChart"),
    { type: "line", data: SalesData}
);