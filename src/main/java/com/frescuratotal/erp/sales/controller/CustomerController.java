package com.frescuratotal.erp.sales.controller;

import com.frescuratotal.erp.common.LogManager;
import com.frescuratotal.erp.inventory.model.Product;
import com.frescuratotal.erp.inventory.model.ProductDao;
import com.frescuratotal.erp.sales.model.Customer;
import com.frescuratotal.erp.sales.model.CustomerCache;
import com.frescuratotal.erp.sales.model.CustomerDao;
import com.frescuratotal.erp.sales.view.CustomerView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerController {

    private ProductDao productDao;
    private CustomerView customerView;
    private CustomerDao customerDao;


    public CustomerController(CustomerDao customerDao, CustomerView customerView, ProductDao productDao) {
        this.customerDao = customerDao;
        this.customerView = customerView;
        this.productDao = productDao;
    }

    public void iniciar() {
        int opcion;
        do {
            opcion = customerView.mostrarMenu();
            switch (opcion) {
                case 1:
                    listarClientes();
                    break;
                case 2:
                    registrarCliente();
                    break;
                case 3:
                    buscarClientePorId();
                    break;
                case 4:
                    importarProductos();
                    break;
                case 0:
                    customerView.mostrarMensaje("Regresando al menu principal...");
                    break;
                default:
                    customerView.mostrarMensaje("Opcion invalida.");
            }
        } while (opcion != 0);
    }

    private void listarClientes() {
        try {
            List<Customer> clientes = customerDao.obtenerClientes();
            customerView.mostrarClientes(clientes);
        } catch (SQLException e) {
            customerView.mostrarMensaje("Error al listar clientes: " + e.getMessage());
        }
    }

    private void registrarCliente() {
        Customer cliente = customerView.pedirDatosCliente();

        if (cliente.getNombre().isEmpty() || cliente.getDocumento().isEmpty()) {
            customerView.mostrarMensaje("El nombre y el documento son obligatorios. Cliente no registrado.");
            return;
        }

        try {
            customerDao.registrarCliente(cliente);
            customerView.mostrarMensaje("Cliente registrado exitosamente.");
            LogManager.registrar("INFO", "Cliente registrado: " + cliente.getNombre());
        } catch (SQLException e) {
            customerView.mostrarMensaje("Error al registrar cliente: " + e.getMessage());
            LogManager.registrar("ERROR", "Error al registrar cliente: " + e.getMessage());
        }
    }

    // Esto pertenece al punto 1 del taller de practica
    private void buscarClientePorId() {
        int idCliente = customerView.pedirIdCliente();
        CustomerCache cache = CustomerCache.getInstance();
        try {
            if (cache.estaEnCache(idCliente)) {
                customerView.mostrarMensaje("(Cliente obtenido de la cache, sin consultar la base de datos)");
            } else {
                customerView.mostrarMensaje("(Cliente consultado en la base de datos)");
            }

            Customer cliente = cache.obtenerCliente(idCliente);
            if (cliente == null) {
                customerView.mostrarMensaje("El cliente con ID " + idCliente + " no fue encontrado.");
            } else {
                customerView.mostrarCliente(cliente);
            }
        } catch (SQLException e) {
            customerView.mostrarMensaje("Error al buscar el cliente: " + e.getMessage());
        }
    }

    private void importarProductos() {
        File archivo = new File("productos_importar.txt");
        if (!archivo.exists()) {
            customerView.mostrarMensaje("Error: No se encontró el archivo productos_importar.txt.");
            return;
        }

        List<Product> productosNuevos = new ArrayList<>();
        boolean hayErrores = false;

        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String cabecera = lector.readLine();
            if (cabecera == null || !cabecera.equals("nombre;descripcion;categoria_id;unidad;precio")) {
                customerView.mostrarMensaje("La cabecera del archivo no es la esperada.");
                hayErrores = true;
            }

            String linea = lector.readLine();
            int numeroLinea = 2;
            while (linea != null && !hayErrores) {
                if (!linea.trim().isEmpty()) {
                    String[] campos = linea.split(";", -1);

                    if (campos.length != 5) {
                        customerView.mostrarMensaje("Linea " + numeroLinea + ": debe tener 5 campos.");
                        hayErrores = true;
                    } else {
                        String nombre = campos[0].trim();
                        String descripcion = campos[1].trim();
                        String categoria_id = campos[2].trim();
                        String unidadMedida = campos[3].trim();
                        String precioBase = campos[4].trim();

                        if (nombre.isEmpty() || descripcion.isEmpty() || categoria_id.isEmpty()
                                || unidadMedida.isEmpty() || precioBase.isEmpty()) {
                            customerView.mostrarMensaje("Linea " + numeroLinea + ": hay campos vacios.");
                            hayErrores = true;
                        }else{
                            Product productos = new Product();
                            int nuevo_id = productos.getIdCategoria();
                            productos.setNombre(nombre);
                            productos.setDescripcion(descripcion);
                            productos.setUnidadMedida(unidadMedida);
                            productos.setIdCategoria(nuevo_id);
                            productosNuevos.add(productos);
                        }
                    }
                }
                linea = lector.readLine();
                numeroLinea++;
            }

            if (hayErrores) {
                customerView.mostrarMensaje("Error: Se encontraron registros inválidos. Importación cancelada.");
                return;
            }

            int total = productDao.obtenerProductos().size();
            customerView.mostrarMensaje("Importación completada. Total de productos registrados: " + total);
            LogManager.registrar("INFO", "Productos importados desde archivo: " + total);
        } catch (IOException e) {
            customerView.mostrarMensaje("Error al leer el archivo: " + e.getMessage());
        } catch (SQLException e) {
            customerView.mostrarMensaje("Error: Se encontraron registros inválidos. Importación cancelada.");
            LogManager.registrar("ERROR", "Error al importar productos: " + e.getMessage());
        }
    }


}
