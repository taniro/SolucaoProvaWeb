package ufrn.br.solucaoprovaweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ufrn.br.solucaoprovaweb.domain.Produto;
import ufrn.br.solucaoprovaweb.repository.ConectaBanco;
import ufrn.br.solucaoprovaweb.repository.ProdutoDAO;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

@Controller
public class ProvaController {

    @RequestMapping("/config")
    public void doConfig(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = ConectaBanco.getConnection();
            stmt = con.
                    prepareStatement(
                    "CREATE TABLE IF NOT EXISTS " +
                            "produto (id SERIAL PRIMARY KEY, " +
                            "nome VARCHAR(55), " +
                            "descricao VARCHAR(55), " +
                            "preco FLOAT)");
            stmt.execute();
            stmt = con
                    .prepareStatement(
                    "INSERT INTO produto (nome, descricao, preco) VALUES \n"
                            + "('Mesa', 'Qualquer', '23.0'),\n"
                            + "('Caneta', 'Qualquer', '52.0'),\n"
                            + "('Cadeira', 'Qualquer', '10.0'),\n"
                            + "('TV', 'Dachshund', '75.0'),\n"
                            + "('Monitor', 'Qualquer', '110.0'),\n"
                            + "('Computador', 'Qualquer', '20.0')");
            stmt.execute();
            con.close();

            response.getWriter().println("ok");

        } catch (SQLException | URISyntaxException ex) {
            response.getWriter().println(ex);
        }
    }

    @GetMapping("/cliente")
    public void doClientePage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var writer = response.getWriter();
        writer.println("<html><body>");
        writer.println("<table>");
        var produtoDao = new ProdutoDAO();
        var listaProdutos = produtoDao.listarProdutos();
        for (var p : listaProdutos) {
            writer.println("<tr>");
            writer.println("<td>");
            writer.println(p.getNome());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(p.getDescricao());
            writer.println("</td>");
            writer.println("<td>");
            writer.println(p.getPreco());
            writer.println("</td>");
            writer.println("<td>");
            writer.println("<a href='/adicionarCarrinho?id=" + p.getId() + "'>Adicionar</a>");
            writer.println("</td>");
            writer.println("</tr>");
        }
        writer.println("</table>");
        writer.println("<a href='/verCarrinho'>Ver Carrinho</a>");
        writer.println("</body></html>");
    }

    @GetMapping("/admin")
    public void doAdminPage(HttpServletResponse response) throws IOException {
        var writer = response.getWriter();
        writer.println("<html><body>");
        writer.println("<form action='/cadastra' method='POST'>" +
                "Nome: <input type='text' name='nome'/> <br />" +
                "Descrição: <input type='text' name='descricao'/> <br />" +
                "Preço: <input type='text' name='preco'/> <br />" +
                "<button type='submit'>Enviar</button>");
        writer.println("</html></body>");
    }

    @PostMapping("/cadastra")
    public void doCadastrar(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Cookie cookie = new Cookie("visita","cookie-value");
        cookie.setMaxAge(60*60*24); //24 horas
        response.addCookie(cookie);

        var nome = request.getParameter("nome");
        var descricao = request.getParameter("descricao");
        var preco = request.getParameter("preco");

        Produto p = new Produto(0, nome, descricao, Float.parseFloat(preco));

        var produtoDao = new ProdutoDAO();
        produtoDao.cadastrarProduto(p);

        response.sendRedirect("/admin");
    }

    @GetMapping("/adicionarCarrinho")
    public void doAdicionarCarrinho(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        ArrayList<Produto> carrinho = (ArrayList<Produto>) session.getAttribute("carrinho");

        var id = request.getParameter("id");

        if (carrinho == null){
            carrinho = new ArrayList<>();
        }

        var produtoDao = new ProdutoDAO();
        Produto p = produtoDao.findProdutoPorId(Integer.parseInt(id));
        if (p != null){
            carrinho.add(p);
        }

        session.setAttribute("carrinho", carrinho);
        var dispatcher = request.getRequestDispatcher("/cliente");
        dispatcher.forward(request, response);
    }

    @GetMapping("/verCarrinho")
    public void doVerCarrinho(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        var writer = response.getWriter();
        writer.println("<html><body>");
        if (session == null  || session.getAttribute("carrinho") == null){
            writer.println("<p>Carrinho vazio</p>");
        }else{
            ArrayList<Produto> carrinho = (ArrayList<Produto>) session.getAttribute("carrinho");
            writer.println("<table>");
            for (var p : carrinho) {
                writer.println("<tr>");
                writer.println("<td>");
                writer.println(p.getNome());
                writer.println("</td>");
                writer.println("<td>");
                writer.println(p.getDescricao());
                writer.println("</td>");
                writer.println("<td>");
                writer.println(p.getPreco());
                writer.println("</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
            writer.println("<a href='/finalizarCarrinho'>Finalizar</a>");
        }
        writer.println("</html></body>");

    }

    @GetMapping("/finalizarCarrinho")
    public void doFinalizarCarrinho(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().invalidate();

        response.sendRedirect("/");
    }
}
