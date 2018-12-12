import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.google.common.io.CharStreams;
import com.google.common.primitives.Ints;

@SuppressWarnings("serial")
@WebServlet("/add")
public class AdditionerServlet extends HttpServlet {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(AdditionerServlet.class.getCanonicalName());

	private static OptionalInt defaultParam2 = OptionalInt.empty();

	private static final int MAX_CONTENT_LENGTH = 100_000;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.TEXT_PLAIN);
		response.setLocale(Locale.FRENCH);

		final OptionalInt param1 = tryParse("param1", request);

		final OptionalInt param2FromRequest = tryParse("param2", request);
		final OptionalInt param2;
		if (param2FromRequest.isPresent()) {
			param2 = param2FromRequest;
		} else {
			/** In this simple example, we neglect multithreading issues. */
			LOGGER.info("Switching to default param2: " + defaultParam2 + ".");
			param2 = defaultParam2;
		}

		if (!param1.isPresent() || !param2.isPresent()) {
			/**
			 * This sends an error in HTML. To stick to plain text, use setStatus instead
			 * and write using response.getWriter to work around a WildFly bug related to
			 * encoding.
			 */
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Exécution impossible, paramètre manquant.");
			return;
		}

		assert param1.isPresent() && param2.isPresent();
		final int int1 = param1.getAsInt();
		final int int2 = param2.getAsInt();
		final int sum = int1 + int2;
		LOGGER.info("Sending sum of " + int1 + " and " + int2 + ": " + sum + ".");

		@SuppressWarnings("resource")
		final ServletOutputStream out = response.getOutputStream();
		out.println("" + sum);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.TEXT_PLAIN);
		response.setLocale(Locale.FRENCH);

		final OptionalInt parsed;
		if (request.getContentLength() == 0 || request.getContentLength() > MAX_CONTENT_LENGTH) {
			parsed = OptionalInt.empty();
		} else {
			@SuppressWarnings("resource")
			final BufferedReader reader = request.getReader();
			final String body = CharStreams.toString(reader);
			final Integer tryParse = Ints.tryParse(body);
			parsed = tryParse == null ? OptionalInt.empty() : OptionalInt.of(tryParse);
		}

		if (!parsed.isPresent()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contenu invalide.");
			return;
		}

		assert parsed.isPresent();
		LOGGER.info("Default param2 set to: " + parsed + ".");
		defaultParam2 = parsed;

		@SuppressWarnings("resource")
		final ServletOutputStream out = response.getOutputStream();
		out.println("ok");
	}

	private OptionalInt tryParse(String parameterName, HttpServletRequest request) {
		final String parameter = request.getParameter(parameterName);
		final Integer parsed = parameter == null ? null : Ints.tryParse(parameter);

		final OptionalInt param;
		if (parsed == null) {
			param = OptionalInt.empty();
			LOGGER.info("No valid " + parameterName + " in request.");
		} else {
			param = OptionalInt.of(parsed);
		}

		return param;
	}
}