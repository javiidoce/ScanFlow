<p align="center"><a href="https://github.com/javiidoce/ScanFlow" target="_blank"><img src="https://i.imgur.com/8TcOm6o.jpeg" width="200" alt="Logo"></a></p>

***

# CoachHub

ScanFlow es una aplicación de Android la cual trata de hacerle la vida más facil a los usuarios que tengan que llevar una gestión de la contabilidad, permitiendoles subir facturas digitalmente o mediante una foto y que estas sean procesadas sin necesidad de que el usuario escriba los datos manualmente.

## Características principales:

- Recopilación de datos eficaz y sencilla.
- Subida de información automática
- Gestión e historial.

## Tecnologías usadas (en principio)
<p align="left"> <img src="https://cdn.worldvectorlogo.com/logos/git-bash.svg" alt="Bash" width="40" height="40"/>  
<img src="https://cdn.worldvectorlogo.com/logos/android-4.svg" alt="Android Studio" width="40" height="40"/> 
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/mysql/mysql-original-wordmark.svg" alt="MySQL" width="40" height="40"/>
<img src="https://camo.githubusercontent.com/4965c61569069e46775836d78ee63f6cc0d8bac40bb2a0e638fe682e5aa18a95/68747470733a2f2f63646e2e776f726c64766563746f726c6f676f2e636f6d2f6c6f676f732f6769746875622d69636f6e2d322e737667" alt="GitHub" width="40" height="40"/>
</p>

```mermaid
pie showData
    title Lenguajes a utilizar en el proyecto
    "Kotlin": 80
    "MySQL": 10
    "Bash": 5
    "GitHub": 5
```
(MySQL o el gestor de base de datos correspondiente que acabe usando)


## Base de datos (en principio)

```mermaid
erDiagram
    categorias {
        int id PK
        string nombre
    }
    facturas {
        int id PK
        string n_factura
        string proveedor
        date fecha_emision
        date fecha_expiracion
        decimal importe_total
        string moneda
        enum estado_procesamiento
        string ruta_archivo_json
        int usuario_id FK
        int categoria_id FK
    }
    lineas_factura {
        int id PK
        int factura_id FK
        text descripcion
        int cantidad
        decimal precio_unitario
        decimal descuento
        decimal total_linea
    }
    procesamientos_ocr {
        int id PK
        int factura_id FK
        timestamp fecha_procesamiento
        text resultado_json
        text observaciones
    }
    usuarios {
        int id PK
        string nombre
        string contraseña_hash
        timestamp fecha_registro
    }

    categorias ||--o{ facturas : "categoria_id"
    usuarios ||--o{ facturas : "usuario_id"
    facturas ||--o{ lineas_factura : "factura_id"
    facturas ||--o{ procesamientos_ocr : "factura_id"
```

## Estado del proyecto 📝

Casi terminado ⌛

## Contacto 💬 

|Correo Electrónico|Número de teléfono|
|------------------|------------------|
|docejavii@gmail.com|+34 640 12 78 42|


## Autor

<a href="https://github.com/javiidoce"> Javier Melendo </a>

## Licencia ⚡

Copyright 2025 Javier Melendo Soler 

Por la presente se concede permiso, libre de cargos, a cualquier persona que obtenga una copia de este software y de los archivos de documentación asociados a utilizar CoachHub sin restricción, incluyendo sin limitación los derechos a usar, copiar, modificar, fusionar, publicar, distribuir, sublicenciar, y/o vender copias de CoachHub, y a permitir a las personas a las que se les proporcione el programa a hacer lo mismo, sujeto a las siguientes condiciones:  El aviso de copyright anterior y este aviso de permiso se incluirán en todas las copias o partes sustanciales del Software.  

EL SOFTWARE SE PROPORCIONA "COMO ESTÁ", SIN GARANTÍA DE NINGÚN TIPO, EXPRESA O IMPLÍCITA, INCLUYENDO PERO NO LIMITADO A GARANTÍAS DE COMERCIALIZACIÓN, IDONEIDAD PARA UN PROPÓSITO PARTICULAR E INCUMPLIMIENTO. EN NINGÚN CASO LOS AUTORES O PROPIETARIOS DE LOS DERECHOS DE AUTOR SERÁN RESPONSABLES DE NINGUNA RECLAMACIÓN, DAÑOS U OTRAS RESPONSABILIDADES, YA SEA EN UNA ACCIÓN DE CONTRATO, AGRAVIO O CUALQUIER OTRO MOTIVO, DERIVADAS DE, FUERA DE O EN CONEXIÓN CON EL SOFTWARE O SU USO U OTRO TIPO DE ACCIONES EN EL SOFTWARE.

<p align="center"><img alt="Markdown" src="https://img.shields.io/badge/markdown-%23000000.svg?style=for-the-badge&logo=markdown&logoColor=white"/></p>
